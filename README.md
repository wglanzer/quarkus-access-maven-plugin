# Quarkus Access Maven Plugin

![Actions](https://img.shields.io/github/actions/workflow/status/wglanzer/quarkus-access-maven-plugin/maven.yml?logo=github)
![Release](https://img.shields.io/github/v/release/wglanzer/quarkus-access-maven-plugin?logo=github)

A Maven plugin to make default Quarkus injection points public. This plugin leverages bytecode manipulation (via Javassist) to expose injection points that are otherwise private.

## Features

- Modifies compiled Quarkus classes to make injection points public.
- Uses Javassist under the hood for bytecode manipulation.
- Easily integrates into your Maven build lifecycle.

## Installation

To use the plugin, add it to your project's `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.github.wglanzer</groupId>
      <artifactId>quarkus-access-maven-plugin</artifactId>
      <version>VERSION</version>
      <executions>
        <execution>
          <goals>
            <goal>modify</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Replace VERSION with the desired version of the plugin.

## Usage
After adding the plugin to your Maven project, you can run it with:

```bash
mvn com.github.wglanzer:quarkus-access-maven-plugin:modify
```

This command will execute the plugin goal and process your Quarkus application, making the defined injection points public. Adjust the configuration in your pom.xml as needed for your specific project requirements.

## Configuration Options

None

## Contributing
Contributions are welcome! If you have suggestions, find a bug, or want to contribute a new feature, please open an issue or submit a pull request.

## License
This project is licensed under the Apache-2.0 License.
