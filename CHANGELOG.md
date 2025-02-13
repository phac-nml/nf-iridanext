# Unreleased

- Added documentation for running test cases.
- Added test cases to verify that missing values in CSV will be encoded as empty strings in IRIDA Next JSON file in the sample metadata section.
- Added test cases for passing missing values in a JSON file.
- Added channel operator `.loadIridaSampleIds()` that will load Irida Next sample IDs from a nf-validation-loaded (or similar) sample sheet (i.e. having a meta object).
- Added test cases for manually loading Irida IDs from a sample sheet.
- Added documentation for manually loading Irida IDs.

# 0.2.0 - 2024/01/22

- Added support for writing JSON output file when using `-resume` in a pipeline.
- Re-structured Nextflow config syntax for loading metadata from CSV file using `csv {}` section in config.
- Added support for reading metadata from JSON file using `json {}` section in config.
- Added support for flattening samples metadata with `iridanext.output.metadata.flatten=true`.
- Added support for validation by custom JSON schema before writing final JSON output file (defaults to no validation).
  - Enable with `iridanext.output.validate=true` and set schema with `iridanext.output.schema=PATH`.
  - If `iridanext.output.schema` is unset, validates against default JSON schema for IRIDA Next output data.
- Added support for `iridanext.output.metadata.{ignore,keep,rename}` to ignore, keep, or rename metadata keys.
- Expanded test suite.

# 0.1.0 - 2023/12/14

- Initial release of plugin enabling the creation of a JSON file containing data to store within [IRIDA Next][irida-next].

[irida-next]: https://github.com/phac-nml/irida-next
