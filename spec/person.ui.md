# Form: Person

The form of `Person`, proposed by `pisa propose-specs` from the domain model. The Salt mockup below is
the layout `pisa generate` renders: reorder the lines, group them in boxes, side by side in one line,
or change the labels. Every field of the model must appear exactly once.

```plantuml
@startsalt
{+
  {
    First Name | "firstName"
    Last Name | "lastName"
    Email | "email"
    Phone | "phone"
    Birthday | "birthday"
    Status | ^status^
    Street | "address.street"
    Zip | "address.zip"
    City | "address.city"
    Company | ^company^
  }
}
@endsalt
```
