# Unreleased

* Added support for writing JSON output file when using `-resume` in a pipeline.
* Re-structured Nextflow config syntax for loading metadata from CSV file using `csv {}` section in config.
* Added support for reading metadata from JSON file using `json {}` section in config.
* Added support for flattening samples metadata with `iridanext.output.metadata.flatten=true`.

# 0.1.0 - 2023/12/14

* Initial release of plugin enabling the creation of a JSON file containing data to store within [IRIDA Next][irida-next].

[irida-next]: https://github.com/phac-nml/irida-next
