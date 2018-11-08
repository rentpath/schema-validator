# schema-validator

schema-validator allows you to version avro schemas in a directory and sync them to an avro schema registry.

It'll return nicer error messages than the registry itself when schemas aren't compatible between each other.

## Building

```
lein uberjar
```

## Usage

```
java -jar schema-validator.jar

schema-validator - checks and/or update proposed avro schemas

Usage: schema-validator [options]

Options:
  -r, --registry-url URL  Avro schema registry url
  -d, --schema-dir PATH   Path to directory of schemas to update
  -u, --update            If set, updates the schemas after checking them. By default, we only check them.
  -h, --help
```

## License

MIT

Copyright © 2018 RentPath, LLC.
