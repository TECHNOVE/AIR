# AIR

Subset of TOML.

## Features

#### Comments for everyone

```toml
# This is a comment
# that spans multiple lines
# and is tied to an empty section, because comments can't exist by themselves
[_head]
```

#### Lists

The only requirement for lists is that each value is on a different line, as well as the ending bracket.

```toml
[section]
  list = [
    "foo"
    "bar"
    "hello"
    "world"
  ]
```

#### Sections and Values

```toml
# Accessible with foo.bar
[foo]
  # nicely indented!
  bar = "foobar"
```

Example of syntax which is **not** allowed:
```toml
# this is not allowed, all values must be in sections
foo = "bar"

# dot syntax is not supported either
foo.bar = "foobar"

# a section can only contain values
[foo]
  [bar]
    foobar = "foobar"
```

These limitations are purposeful. Most user facing configurations do not need to be overly complicated with multiple tiers of objects.

#### Flexible Parser

```toml
[section]

value="hello"



```

Will automatically be fixed as to maintain a consistent style:

```toml
[section]
  value = "hello"
```

#### Easily Updatable

Adding new comments/sections/values is as easy as trying to retrieve them, and AIR will automatically merge your new comments/sections/values with the existing configuration.
This allows users to easily add their own comments and placeholder sections as needed.

## Future Features

- Ability to deprecate sections/values by commenting them
