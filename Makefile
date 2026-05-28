plugin_version := $(shell sed -n "s/^version *= *'\([^']*\)'/\1/p" ./build.gradle)

# Build the plugin
assemble:
	./gradlew assemble

clean:
	rm -rf .nextflow*
	rm -rf work
	rm -rf build
	rm -rf results
	./gradlew clean

# Run plugin unit tests
test:
	./gradlew test

# Install the plugin into local nextflow plugins dir
install:
	./gradlew install

# Publish the plugin
release:
	./gradlew releasePlugin

# Validate the plugin with an example Nextflow pipeline under validation/
# Set NXF_OFFLINE to true so Nextflow does not try to download published versions of the plugin
validate: install
	NXF_OFFLINE=true nextflow run ./validation/ -plugins nf-iridanext@${plugin_version}
