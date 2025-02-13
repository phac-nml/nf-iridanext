# nf-iridanext plugin

This project contains a plugin for integrating Nextflow pipelines with [IRIDA Next][irida-next]. In particular, it will enable a pipeline to produce output consistent with the IRIDA Next [pipeline standards][].

# Getting started

## Scenario 1: Minimal configuration

The following is the minimal configuration needed for this plugin.

**nextflow.config**

```conf
plugins {
    id 'nf-iridanext'
}

iridanext {
    enabled = true
    output {
        path = "${params.outdir}/iridanext.output.json.gz"
    }
}
```

When run with a pipeline (e.g., the [IRIDA Next Example pipeline][iridanextexample]), the configuration will produce the following JSON output in a file `${params.outdir}/iridanext.output.json.gz`.

_Note: `${params.outdir}` is optional and is used in the case where the file should be written to the output directory specified by `--outdir`._

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {}
  }
}
```

This file conforms to the standards as defined in the [IRIDA Next Pipeline Standards][iridanext-pipeline-standards] document.

## Scenario 2: Including files

To include files to be saved within IRIDA Next, you can define path match expressions under the `iridanext.output.files` section. The **global** section is used for global output files for the pipeline while the **samples** is used for output files associated with particular samples (matching to sample identifiers is automatically performed).

**nextflow.config**

```conf
plugins {
    id 'nf-iridanext'
}

iridanext {
    enabled = true
    output {
        path = "${params.outdir}/iridanext.output.json.gz"
        overwrite = true
        files {
            global = ["**/summary/summary.txt.gz"]
            samples = ["**/assembly/*.assembly.fa.gz"]
        }
    }
}
```

This configuration will produce the following example JSON output:

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [{ "path": "summary/summary.txt.gz" }],
    "samples": {
      "SAMPLE1": [{ "path": "assembly/SAMPLE1.assembly.fa.gz" }],
      "SAMPLE3": [{ "path": "assembly/SAMPLE3.assembly.fa.gz" }],
      "SAMPLE2": [{ "path": "assembly/SAMPLE2.assembly.fa.gz" }]
    }
  },
  "metadata": {
    "samples": {}
  }
}
```

Files are matched to samples using the `meta.id` map used by [nf-core formatted modules][nf-core-meta-map]. The matching key (`id` in `meta.id`) can be overridden by setting:

```conf
iridanext.output.files.idkey = "newkey"
```

## Scenario 3: Including metadata

Metadata associated with samples can be included by filling in the the `iridanext.output.metadata.samples` section, like below:

**nextflow.config**

```conf
plugins {
    id 'nf-iridanext'
}

iridanext {
    enabled = true
    output {
        path = "${params.outdir}/iridanext.output.json.gz"
        overwrite = true
        metadata {
            samples {
                csv {
                    path = "**/output.csv"
                    idcol = "column1"
                }
            }
        }
    }
}
```

This will parse a CSV file for metadata. The `csv.path` keyword specifies the file to parse. The `csv.idcol` defines the column that should match to the sample identifiers.

If there exists an example CSV file like the following:

**output.csv**
| column1 | b | c |
|--|--|--|
| SAMPLE1 | 2 | 3 |
| SAMPLE2 | 4 | 5 |
| SAMPLE3 | 6 | 7 |

Then running the pipeline will produce an output like the following:

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": { "b": "2", "c": "3" },
      "SAMPLE2": { "b": "4", "c": "5" },
      "SAMPLE3": { "b": "6", "c": "7" }
    }
  }
}
```

The CSV parser will only include metadata in the final output JSON for sample identifiers in the CSV file (defined in the column specified by `csv.idcol`) that match to sample identifiers in the pipeline [meta map][nf-core-meta-map] (the key in the meta map defined using `iridanext.output.files.idkey`).

### JSON metadata

If, instead of parsing a CSV file, you wish to parse metadata from a JSON file, then you can replace the `csv {}` configuration section above with:

```conf
json {
    path = "**/output.json"
}
```

For example, a JSON file like the following:

**output.json**

```json
{
  "SAMPLE1": {
    "key1": "value1",
    "key2": ["a", "b"]
  },
  "SAMPLE2": {
    "key1": "value2"
  }
}
```

Would result in the following output:

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": { "key1": "value1" },
      "SAMPLE2": { "key2": ["a", "b"] }
    }
  }
}
```

### Flatten metadata

Setting the configuration value `iridanext.output.metadata.samples.flatten = true` will flatten the metadata JSON to a single level of key/value pairs (using dot `.` notation for keys).

The two scenarios show the difference between `flatten = false` (default) and `flatten = true`.

#### flatten = false

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": {
        "key1": {
          "subkey1": "value1",
          "subkey2": "value2"
        }
      },
      "SAMPLE2": {
        "key2": ["a", "b"]
      }
    }
  }
}
```

#### flatten = true

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": {
        "key1.subkey1": "value1",
        "key1.subkey2": "value2"
      },
      "SAMPLE2": {
        "key2.1": "a",
        "key2.2": "b"
      }
    }
  }
}
```

### Adjust saved metadata

The `iridanext.output.metadata.samples.{ignore,keep,rename}` configuration options can be used to adjust what is stored within the metadata JSON structure.

_Note: If `flatten=true` is enabled, then the metadata key names here refer to the flattened names._

#### ignore

Setting `iridanext.output.metadata.samples.ignore = ["b"]` in the config (like below) will cause the metadata with the key _b_ to be ignored in the final IRIDA Next output JSON file.

For example, in the config below:

**nextflow.config**

```config
plugins {
    id 'nf-iridanext'
}

iridanext {
    enabled = true
    output {
        path = "${params.outdir}/iridanext.output.json.gz"
        overwrite = true
        metadata {
            samples {
                ignore = ["b"]
                csv {
                    path = "**/output.csv"
                    idcol = "column1"
                }
            }
        }
    }
}
```

If this used to load the below CSV file.

**output.csv**
| column1 | b | c |
|--|--|--|
| SAMPLE1 | 2 | 3 |
| SAMPLE2 | 4 | 5 |
| SAMPLE3 | 6 | 7 |

Then an output like below is produced (that is, the _b_ column is ignored).

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": { "c": "3" },
      "SAMPLE2": { "c": "5" },
      "SAMPLE3": { "c": "7" }
    }
  }
}
```

#### keep

Setting `iridanext.output.metadata.samples.keep = ["b"]` is similar to the ignore case, except the listed columns will be kept.

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": { "b": "2" },
      "SAMPLE2": { "b": "4" },
      "SAMPLE3": { "b": "6" }
    }
  }
}
```

#### rename

Setting `iridanext.output.metadata.samples.rename` will rename the listed keys to new key names (specified as a Map). For example:

**nextflow.config**

```config
plugins {
    id 'nf-iridanext'
}

iridanext {
    enabled = true
    output {
        path = "${params.outdir}/iridanext.output.json.gz"
        overwrite = true
        metadata {
            samples {
                rename = ["b": "b_col"]
                csv {
                    path = "**/output.csv"
                    idcol = "column1"
                }
            }
        }
    }
}
```

**iridanext.output.json.gz**

```json
{
  "files": {
    "global": [],
    "samples": {}
  },
  "metadata": {
    "samples": {
      "SAMPLE1": { "b_col": "2", "c": "3" },
      "SAMPLE2": { "b_col": "4", "c": "5" },
      "SAMPLE3": { "b_col": "6", "c": "7" }
    }
  }
}
```

### Missing values in metadata

There are two different scenarios where metadata key/value pairs could be missing for a sample, which result in different behaviours in IRIDA Next.

1. **Ignore key**: If the `key` is left out of the samples metadata in the IRIDA Next JSON, then nothing is written for that `key` for the sample. Any existing metadata under that `key` will remain in IRIDA Next.

2. **Delete key**: If a metadata value is an empty string (`"key": ""`) or null (`"key": null`), then IRIDA Next will remove that particular metadata key/value pair from the sample metadata if it exists. This is the expected scenario if pipeline results contain missing (or N/A) values (deleting older metadata keys prevents mixing up old and new pipeline analysis results in the metadata table).

The following are the expectations for writing missing values in the final IRIDA Next JSON file (in order to delete the key/value pairs in IRIDA Next).

#### Encoding missing metadata values using JSON

If the metadata key `b` for **SAMPLE1** is encoded as an empty string `""` or `null` in the JSON file like the below example:

**output.json**
```json
{
  "SAMPLE1": {
    "a": "value1",
    "b": ""
  }
}
```

Then the final IRIDA Next JSON file will preserve the empty string/null value in the samples metadata section:

**iridanext.output.json.gz**
```json
"metadata": {
  "samples": {
    "SAMPLE1": { "a": "value1", "b": "" }
  }
}
```

#### Encoding missing metadata values using CSV

If the metadata key `b` for **SAMPLE1** is left empty in the CSV file like the below two examples:

**output.csv** as table
| column1 | b | c |
|--|--|--|
| SAMPLE1 |  | 3 |
| SAMPLE2 | 4 | 5 |
| SAMPLE3 | 6 | 7 |

**output.csv** as CSV
```
column1,b,c
SAMPLE1,,3
SAMPLE2,4,5
Sample3,6,7
```

Then the value for `b` for **SAMPLE1** will be written as an empty string in the IRIDA Next JSON file:

**iridanext.output.json.gz**
```json
"metadata": {
  "samples": {
    "SAMPLE1": { "b": "", "c": "3" },
    "SAMPLE2": { "b": "4", "c": "5" },
    "SAMPLE3": { "b": "6", "c": "7" }
  }
}
```

### Manually loading sample IDs

The default behaviour of the plugin is to automatically observe completed tasks and identify the sample ID of the `meta` object associated with it. This sample ID is recorded and used when producing the metadata section in the `iridanext.output.json.gz` output.

This process will fail to load any sample IDs that were not submitted to a task individually. For example, if a sample is submitted to an assembly task in the following way, then it will be included as a valid sample ID for the whole workflow:

Workflow:
```
// input: tuple(meta, [ file(fastq_1), file(fastq_2) ])
ASSEMBLY (input)
```

Process:
```
process ASSEMBLY {
    input:
    tuple val(meta), path(reads)
    (...)
}
```

However, if samples are only ever submitted to a process in a way similar to as follows, then these sample IDs will not be automatically detected:

Workflow:
```
// input: tuple(meta, [ file(fastq_1), file(fastq_2) ])

metadata = input.map {
    meta, reads ->
    tuple(meta.id, meta.metadata_1, meta.metadata_2)}

REPORT(metadata.toList())
```

Process:
```
process REPORT {
    input:
    val metadata
    (...)
}
```

In this situation, the solution is to use the provided `loadIridaSampleIds()` method in the workflow as follows:

```
// input: tuple(meta, [ file(fastq_1), file(fastq_2) ])
loaded_input = input.loadIridaSampleIds()
```

Or when using the `nf-validation` plugin, in a manner similar to the following:

```
input = Channel.fromSamplesheet("input")
               .loadIridaSampleIds()
```

However, be careful not to use the function in a way that would create a race condition, as the sample IDs may not be loaded correctly:

```
input = Channel.fromSamplesheet("input")
input = input.map { ... } // some input transformation
input.loadIridaSampleIds()
```

Since Nextflow tries to parallelize as much as possible, this will cause `input = input.map { ... }` and `input.loadIridaSampleIds()` to be run simultaneously, which will create a race condition and possibly undesirable behaviour.

# Development

In order to build this plugin you will need a Java Development Kit (such as [OpenJDK](https://openjdk.org/)) and [Groovy](https://groovy-lang.org/index.html). For Ubuntu, this can be installed with:

```bash
sudo apt install default-jdk groovy
```

## Build and install from source

In order to build and install the plugin from source, please do the following:

### 1. Build

```bash
git clone https://github.com/phac-nml/nf-iridanext.git
cd nf-iridanext
make buildPlugins
```

Please see the [Nextflow plugins documentation][nextflow-develop-plugins] and the [nf-hello][] example plugin for more details.

### 2. Install

```bash
cp -r build/plugins/nf-iridanext-0.3.0 ~/.nextflow/plugins
```

This copies the compiled plugin files into the Nextflow plugin cache (default `~/.nextflow/plugins`). Please change the version `0.3.0` to the version of the plugin built from source.

### 3. Use

In order to use the built plugin, you have to specify the exact version in the Nextflow configuration so that Nextflow does not try to update the plugin. That is, in the configuration use:

```conf
plugins {
    id 'nf-iridanext@0.3.0'
}
```

## Run unit/integration tests

In order to run the test cases, please clone this repository and run the following command:

```bash
./gradlew check
```

To get more information for any failed tests, please run:

```bash
./gradlew check --info
```

# Example: nf-core/fetchngs

One use case of this plugin is to structure reads and metadata downloaded from NCBI/ENA for storage in IRIDA Next by making use of the [nf-core/fetchngs][nf-core/fetchngs] pipeline. The example configuration [fetchngs.conf][] can be used for this purpose. To test, please run the following (using [ids.csv][fetchngs-ids.csv] as example data accessions):

```bash
# Download config and SRA accessions
wget https://raw.githubusercontent.com/phac-nml/nf-iridanext/main/docs/examples/fetchngs/fetchngs.conf
wget https://raw.githubusercontent.com/phac-nml/nf-iridanext/main/docs/examples/fetchngs/ids.csv

nextflow run nf-core/fetchngs -profile singularity --outdir results --input ids.csv -c fetchngs.conf
```

This will produce the following output: [iridanext.output.json][fetchngs-out].

# Credits

This plugin was developed based on the `nf-hello` Nextflow plugin template <https://github.com/nextflow-io/nf-hello>. Other sources of information for development include the [nf-prov][] and [nf-validation][] Nextflow plugins, as well as the [Nextflow documentation][nextflow-docs].

# Legal

Copyright 2023 Government of Canada

Original nf-hello project Copyright to respective authors

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this work except in compliance with the License. You may obtain a copy of the
License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

[irida-next]: https://github.com/phac-nml/irida-next
[pipeline standards]: https://github.com/phac-nml/pipeline-standards
[nf-prov]: https://github.com/nextflow-io/nf-prov
[nf-validation]: https://github.com/nextflow-io/nf-validation
[nextflow-docs]: https://www.nextflow.io/docs/latest/index.html
[iridanext-pipeline-standards]: https://github.com/phac-nml/pipeline-standards?tab=readme-ov-file#4-output
[iridanextexample]: https://github.com/phac-nml/iridanextexample
[nf-core-meta-map]: https://nf-co.re/docs/contributing/modules#what-is-the-meta-map
[nf-core/fetchngs]: https://nf-co.re/fetchngs
[fetchngs.conf]: docs/examples/fetchngs/fetchngs.conf
[fetchngs-ids.csv]: docs/examples/fetchngs/ids.csv
[fetchngs-out]: docs/examples/fetchngs/iridanext.output.json
[nextflow-develop-plugins]: https://www.nextflow.io/docs/latest/developer/plugins.html
[nf-hello]: https://github.com/nextflow-io/nf-hello
