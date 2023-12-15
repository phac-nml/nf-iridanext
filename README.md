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

*Note: `${params.outdir}` is optional and is used in the case where the file should be written to the output directory specified by `--outdir`.*

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
        "global": [{"path": "summary/summary.txt.gz"}],
        "samples": {
            "SAMPLE1": [{"path": "assembly/SAMPLE1.assembly.fa.gz"}],
            "SAMPLE3": [{"path": "assembly/SAMPLE3.assembly.fa.gz"}],
            "SAMPLE2": [{"path": "assembly/SAMPLE2.assembly.fa.gz"}]}
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
                path = "**/output.csv"
                id = "column1"
            }
        }
    }
}
```

The `metadata.samples.path` keyword specifies a file to parse for metadata (only CSV parsing is supported now). The `metadata.samples.id` defines the column that should match to the sample identifiers.

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
            "SAMPLE1": {"b": "2","c": "3"},
            "SAMPLE2": {"b": "4","c": "5"},
            "SAMPLE3": {"b": "6","c": "7"}
        }
    }
}
```

# Development

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
cp -r build/plugins/nf-iridanext-0.2.0 ~/.nextflow/plugins
```

This copies the compiled plugin files into the Nextflow plugin cache (default `~/.nextflow/plugins`). Please change the version `0.1.0` to the version of the plugin built from source.

### 3. Use

In order to use the built plugin, you have to specify the exact version in the Nextflow configuration so that Nextflow does not try to update the plugin. That is, in the configuration use:

```conf
plugins {
    id 'nf-iridanext@0.2.0'
}
```

# Example: nf-core/fetchngs

One use case of this plugin is to structure reads and metadata downloaded from NCBI/ENA for storage in IRIDA Next by making use of the [nf-core/fetchngs][nf-core/fetchngs] pipeline. The example configuration [fetchngs.conf][] can be used for this purpose. To test, please run the following (using [ids.csv][fetchngs-ids.csv] as example data accessions):

```bash
# Tell Nextflow where to get plugin since it's not part of Nextflow plugins index yet
export NXF_PLUGINS_TEST_REPOSITORY="https://github.com/phac-nml/nf-iridanext/releases/download/0.1.0/nf-iridanext-0.1.0-meta.json"

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
