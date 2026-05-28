# Validation

To run the validation nextflow pipeline you will need to first build and install the pipeline code.

```bash
make clean install
```

You will also need to modify [nextflow.config](nextflow.config) and update the installed plugin version to the one found in [build.gradle](../build.gradle). For example, update `nf-iridanext@0.3.0` to `nf-iridanext@0.4.0`. The reason for setting the exact version of the `nf-iridanext` plugin to use for the validation pipeline is that otherwise, Nextflow will try to download the latest public version of `nf-iridanext` instead of using the latest plugin code from this repository.

Next, you can run:

```bash
nextflow run validation/
```

See also the GitHub Actions script for how this validation pipeline is tested in GitHub: [.github/workflows/build.yml](../.github/workflows/build.yml).

