// List + form component shared by the generated pages (Alpine.js). Each page sets
// window.PISA_PAGE: the REST path, the id, and how every form field maps to JSON. Field names are
// paths, so a composed part ("address.street") is read and written as a nested object. The list is
// read one page at a time (GET api/<plural>?page=&size=), and so is every table of an inverse
// collection in the form (GET api/<plural>/<id>/<collection>?page=&size=). A link to
// <page>.html#edit=<id> opens that row in the form, <page>.html#view=<id> shows it read-only.
const getPath = (object, path) => path.split(".").reduce((o, key) => (o === null || o === undefined ? undefined : o[key]), object);
function setPath(object, path, value) {
  const keys = path.split(".");
  const last = keys.pop();
  let target = object;
  for (const key of keys) {
    if (target[key] === null || target[key] === undefined) target[key] = {};
    target = target[key];
  }
  target[last] = value;
}

document.addEventListener("alpine:init", () => {
  Alpine.data("crud", (page) => ({
    items: [],
    // Where the list is: the current page (from 0), how many pages and rows there are.
    list: { page: 0, pages: 0, total: 0 },
    // Per table of an inverse collection: its rows on the current page and the same counters.
    tables: {},
    refs: {},
    form: {},
    // Id of the row being edited or viewed; null while creating a new one.
    editing: null,
    // The row is shown read-only (a click on a row): the form is locked and has an Edit button for whoever may change it.
    viewing: false,
    // Field path -> message for the fields that are wrong (found here before sending, or named by the
    // server in a 400 answer); the page marks them red.
    errors: {},
    // Ids of the composed parts of the row being edited (path -> id), so saving updates them in
    // place instead of replacing them.
    partIds: {},
    error: "",
    // Who is signed in and what they may do (GET api/me): permissions by entity name -> operations.
    me: null,

    async init() {
      await this.loadMe();
      for (const ref of page.refs) this.refs[ref.key] = [];
      this.reset();
      await this.load();
      const match = /^#(edit|view)=(.+)$/.exec(location.hash);
      if (match) {
        try {
          const item = await this.request("GET", `${page.api}/${match[2]}`);
          if (match[1] === "view") this.view(item);
          else this.edit(item);
        } catch (e) {
          this.error = e.message;
        }
      }
    },

    async request(method, url, body) {
      const options = { method, headers: {} };
      // The server sets a cookie with the token against forged requests; it wants it back as a header on a change.
      const token = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
      if (token) options.headers["X-XSRF-TOKEN"] = decodeURIComponent(token.split("=")[1]);
      if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
      }
      const response = await fetch(url, options);
      // The session is over (or never was): to the login.
      if (response.status === 401) {
        location.href = "login.html";
        throw new Error("Please sign in.");
      }
      if (!response.ok) {
        let message = `${response.status} ${response.statusText}`;
        let fields = {};
        try {
          const data = await response.json();
          message = data.error || data.message || message;
          // A value that breaks a rule of the model: the server names the fields (path -> message).
          fields = data.fields || {};
        } catch (ignored) {
          // no JSON body - keep the status line
        }
        const failure = new Error(message);
        failure.fields = fields;
        throw failure;
      }
      return response.status === 204 ? null : response.json();
    },

    async load() {
      try {
        let result = await this.request("GET", `${page.api}?page=${this.list.page}&size=${page.pageSize}`);
        // The last row of the last page was deleted: show the page before.
        if (result.items.length === 0 && result.page > 0) {
          result = await this.request("GET", `${page.api}?page=${Math.max(result.pages - 1, 0)}&size=${page.pageSize}`);
        }
        this.items = result.items;
        this.list = { page: result.page, pages: result.pages, total: result.total };
        for (const ref of page.refs) this.refs[ref.key] = await this.request("GET", ref.api);
      } catch (e) {
        this.error = e.message;
      }
    },

    async loadMe() {
      this.me = await (await fetch("api/me")).json();
    },

    // Whether the signed-in user may do `operation` with this page's entity (what the roles in security.yml allow).
    can(operation) {
      return !this.me || (this.me.permissions[page.entity] || []).includes(operation);
    },

    async goTo(number) {
      this.list.page = number;
      await this.load();
    },

    emptyTable() {
      return { items: [], page: 0, pages: 0, total: 0 };
    },

    async loadTable(key, number) {
      try {
        const result = await this.request(
          "GET",
          `${page.api}/${encodeURIComponent(this.editing)}/${key}?page=${number}&size=${page.pageSize}`
        );
        this.tables[key] = { items: result.items, page: result.page, pages: result.pages, total: result.total };
      } catch (e) {
        this.error = e.message;
      }
    },

    reset() {
      this.editing = null;
      this.viewing = false;
      this.partIds = {};
      this.error = "";
      this.errors = {};
      for (const key of page.tables) this.tables[key] = this.emptyTable();
      const form = {};
      if (!page.idGenerated) form[page.id] = "";
      for (const field of page.fields) {
        setPath(form, field.name, field.kind === "boolean" ? false : field.kind === "refs" ? [] : "");
      }
      this.form = form;
    },

    // A click on a row: the entry in the form, locked. Whoever may change it gets an Edit button (startEdit).
    view(item) {
      this.edit(item);
      this.viewing = true;
    },

    startEdit() {
      this.viewing = false;
    },

    edit(item) {
      this.error = "";
      this.viewing = false;
      this.editing = item[page.id];
      this.partIds = {};
      for (const part of page.parts) {
        const id = getPath(item, `${part.path}.${part.idName}`);
        if (id !== null && id !== undefined) this.partIds[part.path] = id;
      }
      const form = { [page.id]: item[page.id] };
      for (const field of page.fields) {
        const value = getPath(item, field.name);
        if (field.kind === "ref") setPath(form, field.name, value ? String(value[field.idName]) : "");
        else if (field.kind === "refs") setPath(form, field.name, (value || []).map((v) => String(v[field.idName])));
        else if (field.kind === "boolean") setPath(form, field.name, Boolean(value));
        else setPath(form, field.name, value === null || value === undefined ? "" : value);
      }
      this.form = form;
      for (const key of page.tables) {
        this.tables[key] = this.emptyTable();
        this.loadTable(key, 0);
      }
    },

    payload() {
      const body = {};
      if (!page.idGenerated && this.editing === null) {
        body[page.id] = page.idNumber ? Number(this.form[page.id]) : this.form[page.id];
      }
      const refId = (field, id) => ({ [field.idName]: field.idNumber ? Number(id) : id });
      for (const field of page.fields) {
        const value = getPath(this.form, field.name);
        let json;
        if (field.kind === "ref") json = value === "" ? null : refId(field, value);
        else if (field.kind === "refs") json = value.map((id) => refId(field, id));
        else if (field.kind === "number") json = value === "" || value === null ? null : Number(value);
        else if (field.kind === "boolean") json = Boolean(value);
        else json = value === "" ? null : value;
        setPath(body, field.name, json);
      }
      for (const part of page.parts) {
        if (this.partIds[part.path] !== undefined) setPath(body, `${part.path}.${part.idName}`, this.partIds[part.path]);
      }
      return body;
    },

    // The rules of the model, checked before anything is sent (the server checks them again): required,
    // length, range, pattern, and an email address or URL with validator.js. Returns path -> message.
    validate() {
      const errors = {};
      if (!page.idGenerated && this.editing === null && String(this.form[page.id] ?? "").trim() === "") {
        errors[page.id] = "is required";
      }
      for (const field of page.fields) {
        const rules = field.rules;
        if (!rules) continue;
        const value = getPath(this.form, field.name);
        const text = value === null || value === undefined ? "" : String(value);
        if (text === "") {
          if (rules.required) errors[field.name] = "is required";
          continue;
        }
        if (rules.minlength !== undefined && text.length < rules.minlength) errors[field.name] = `needs at least ${rules.minlength} characters`;
        else if (rules.maxlength !== undefined && text.length > rules.maxlength) errors[field.name] = `can have at most ${rules.maxlength} characters`;
        else if (rules.min !== undefined && Number(text) < rules.min) errors[field.name] = `must be at least ${rules.min}`;
        else if (rules.max !== undefined && Number(text) > rules.max) errors[field.name] = `must be at most ${rules.max}`;
        else if (rules.format === "email" && !validator.isEmail(text)) errors[field.name] = "must be a well-formed email address";
        else if (rules.format === "url" && !validator.isURL(text, { require_protocol: true, protocols: ["http", "https"] })) errors[field.name] = "must be a URL starting with http:// or https://";
        else if (rules.pattern !== undefined && !new RegExp(`^(?:${rules.pattern})$`).test(text)) errors[field.name] = "has the wrong format";
      }
      return errors;
    },

    async save() {
      if (this.viewing) return;
      this.errors = this.validate();
      if (Object.keys(this.errors).length > 0) {
        this.error = "Some values are not valid.";
        return;
      }
      try {
        if (this.editing === null) await this.request("POST", page.api, this.payload());
        else await this.request("PUT", `${page.api}/${encodeURIComponent(this.editing)}`, this.payload());
        this.reset();
        await this.load();
      } catch (e) {
        this.error = e.message;
        this.errors = e.fields || {};
      }
    },

    async remove(item) {
      if (!confirm("Delete this entry?")) return;
      try {
        await this.request("DELETE", `${page.api}/${encodeURIComponent(item[page.id])}`);
        if (this.editing === item[page.id]) this.reset();
        await this.load();
      } catch (e) {
        this.error = e.message;
      }
    },

    // A referenced row as text: its label member, else its id.
    label(row, labelName, idName) {
      if (!row) return "";
      return row[labelName] !== null && row[labelName] !== undefined ? String(row[labelName]) : String(row[idName]);
    },

    show(value) {
      if (value === null || value === undefined) return "";
      if (value === true) return "yes";
      if (value === false) return "no";
      return String(value);
    },
  }));

  // The menu in the navigation (hamburger): who is signed in, change the password, sign out.
  Alpine.data("userMenu", () => ({
    me: null,
    open: false,

    async init() {
      const response = await fetch("api/me");
      this.me = response.ok ? await response.json() : null;
    },

    async logout() {
      const token = document.cookie.split("; ").find((c) => c.startsWith("XSRF-TOKEN="));
      await fetch("logout", { method: "POST", headers: { "X-XSRF-TOKEN": token ? decodeURIComponent(token.split("=")[1]) : "" } });
      location.href = "login.html";
    },
  }));
});
