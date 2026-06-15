# rimfrost-regel-bekraftabeslut changelog

Changelog of rimfrost-regel-bekraftabeslut.

## 1.1.4 (2026-06-15)

### Bug Fixes

-  Bump rimfrost-framework-regel-manuell version ([c8490](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/c8490e534a83133) Lars Persson)  

## 1.1.3 (2026-06-04)

## rimfrost-1.1 (2026-06-04)

### Bug Fixes

-  Bump dependency versions ([066aa](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/066aa7046093b04) Lars Persson)  
-  Remove unnecessary dependencies from pom.xml ([7599e](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/7599e0022d482f2) Lars Persson)  

## 1.1.1 (2026-05-26)

### Bug Fixes

-  replace JUnit 4 Assert import with JUnit 5 Assertions ([ddf88](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/ddf88cc6288ff90) Ulf Slunga)  
-  fix pom.xml dependency indentation and ordering ([f4976](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/f49761a5109e777) Ulf Slunga)  
-  remove redundant test properties already defined in src/main ([39608](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/396083fd0bdab63) Ulf Slunga)  
-  bump regel-manuell to 1.0.11, add erbjudande wiring and OUL REST stubs ([9b6fd](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/9b6fdeab3f8c1b0) Ulf Slunga)  
-  update tests for OUL REST — replace Status and waitForOulStatusMessage ([de216](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/de2168a8cdbcf9f) Ulf Slunga)  
-  bump rimfrost-framework-regel-manuell to 1.0.9 ([573d4](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/573d4a6723a2cfc) Ulf Slunga)  
-  remove obsolete Kafka OUL request/response steps from SequenceTest ([8f5e4](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/8f5e4bebcfc04a4) Ulf Slunga)  
-  switch OUL interface from Kafka to REST ([86700](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/86700b86c488dac) Ulf Slunga)  

## 1.1.0 (2026-05-19)

### Features

-  Remove redundant index-dependency entries covered by rimfrost-framework-regel-manuell ([6d75f](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/6d75ff23f7bbbb0) Ulf Slunga)  
-  Add waitForRegelManuellReady to GET and PATCH tests to avoid race conditions ([b5c90](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/b5c9021e40a43db) Ulf Slunga)  

### Bug Fixes

-  Migrate storage to framework ManuellRegelCommonDataStorage with PostgreSQL persistence ([fc59c](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/fc59c2171d24b1f) Ulf Slunga)  
-  Bump rimfrost-framework-regel-manuell version ([fe6cc](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/fe6cc1cc2eafa95) Lars Persson)  
-  Add try-catch handling with logging at places where an exception may be thrown ([1cbcc](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/1cbcc76f3b4e63f) Lars Persson)  
-  Remove superfluous semicolon ([e0002](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/e0002cb20413c3c) Lars Persson)  
-  Bump rimfrost-framework-regel-manuell version ([73ea3](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/73ea3d45b87c565) Lars Persson)  
-  Handle exceptions from external dependencies ([96ad2](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/96ad2845dd2cb20) Lars Persson)  

## 1.0.0 (2026-04-29)

### Breaking changes

-  release 1.0 ([8c6de](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/8c6def6da5819bb) Lars Persson)  

### Features

-  release 1.0 ([8c6de](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/8c6def6da5819bb) Lars Persson)  

### Bug Fixes

-  Bump apis to released versions ([43b67](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/43b670b25d0ca10) Lars Persson)  
-  bump framework-regel-manuell version ([7b2de](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/7b2de709f33066d) Nils Elveros)  
-  Split test into multiple to align with rtf-manuell test structure ([1cf26](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/1cf260325cb01de) Lars Persson)  
-  Bump rimfrost-framework-regel-manuell version ([c6d4f](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/c6d4f1cf86014f1) Lars Persson)  

### Other changes

**removed kafka.subtopic from test properties**


[8ea53](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/8ea53d1c78311db) Nils Elveros *2026-04-23 09:14:31*


## 0.0.10 (2026-04-17)

### Bug Fixes

-  Use ErsattningData from rimfrost-ersattning-data ([db4c2](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/db4c207364c59ac) Lars Persson)  
-  Use referensdata adapter ([a0caf](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/a0cafb3c7c1d3c2) Lars Persson)  

## 0.0.9 (2026-04-08)

### Bug Fixes

-  correct url ([eea8c](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/eea8c9380d4e08a) Nils Elveros)  

## 0.0.8 (2026-04-02)

### Bug Fixes

-  Bump rimfrost-framework-regel-manuell version ([39392](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/39392529516b8d7) Lars Persson)  
-  added produceratresultatrefs ([605b4](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/605b4c1018861ac) Nils Elveros)  
-  bump version ([0a468](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/0a4684ae3451fee) Nils Elveros)  
-  Bump rimfrost-framework-regel-manuell version ([f0ae3](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/f0ae3d076bd3bbd) Lars Persson)  
-  Use non-framework version of adapters ([77dd5](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/77dd5414f96146c) Lars Persson)  
-  Add support for sending beslut ([83935](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/839354b8e4edbee) Lars Persson)  
-  Replace existing object with updated object in ProduceradeResultat ([b9485](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/b948539a48c55a5) Lars Persson)  
-  **deps**  update dependency se.fk.maven:fk-maven-quarkus-parent to v1.12.0 ([a07fc](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/a07fc4d4dd8deff) renovate[bot])  
-  Bump framework version ([be74f](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/be74f70a3820e66) Lars Persson)  
-  Bump framework version ([1b8bf](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/1b8bf2db187187e) Lars Persson)  
-  **deps**  update dependency org.yaml:snakeyaml to v2.6 ([9af7e](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/9af7e5a9ab9c69b) renovate[bot])  

## 0.0.7 (2026-03-05)

### Bug Fixes

-  path name bekraftabeslut ist för bekrafta-beslut ([90692](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/9069264087c557d) Ulf Slunga)  

## 0.0.6 (2026-03-05)

### Bug Fixes

-  topic name bekraftabeslut ist för bekrafta-beslut ([f1c76](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/f1c760024447165) Ulf Slunga)  

## 0.0.5 (2026-03-04)

### Bug Fixes

-  Rename kundbehovsflode to handlaggning ([a4325](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/a4325f5873c2b86) Lars Persson)  

## 0.0.4 (2026-03-03)

### Bug Fixes

-  Bump to trigger release flow ([a1c7f](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/a1c7fd9820cde58) Lars Persson)  
-  **deps**  update dependency se.fk.rimfrost.regel.bekraftabeslut.openapi:rimfrost-regel-bekraftabeslut-openapi-jaxrs-spec to v0.0.1 ([579a7](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/579a78e98577878) renovate[bot])  
-  Bump rimfrost-framework-regel-manuell version to include upstream bugfix ([d5bad](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/d5badfe7c32be84) Lars Persson)  

## 0.0.3 (2026-02-25)

### Bug Fixes

-  Bump rimfrost-framework-regel-manuell version to include upstream bugfix ([402fb](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/402fb98984f9e1e) Lars Persson)  

## 0.0.2 (2026-02-24)

### Bug Fixes

-  Bump rimfrost-framework-regel-manuell version ([bfeaa](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/bfeaa9070a52885) Lars Persson)  
-  Use getRegelData from CommonRegelData ([45393](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/45393af81f8380b) Lars Persson)  
-  Use rimfrost-framework-storage ([28b89](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/28b89e3df6cbf38) Lars Persson)  
-  update framework-regel-manuel to 0.1.10 ([19bb5](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/19bb5e9ae50df58) Nils Elveros)  
-  Bump rimfrost-framework-regel-manuell version ([4b2c1](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/4b2c1f2261762f9) Lars Persson)  
-  Använder adapters för folkbokford och arbetsgivare ([045da](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/045da9f1e6d42dc) Ulf Slunga)  
-  Bump rimfrost-framework-regel-manuell version ([b412d](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/b412d7a0a688afb) Lars Persson)  
-  Add extended task desiption endpoint ([e182a](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/e182a5305c6a322) Lars Persson)  
-  removed legacy dependencies ([a54f2](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/a54f2778047eef9) Nils Elveros)  
-  Implement decideUtfall that was introduced in rimfrost-framework-regel-manuell PR #7 ([ff6c1](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/ff6c1eef19368ff) Lars Persson)  
-  update so we send the replyTo header in oul request ([e1b23](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/e1b23fd8e16658f) Nils Elveros)  
-  ErsättningData och Underlag från rimfrost-framework-regel ([f2a94](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/f2a94d0da0fcb2e) Ulf Slunga)  
-  handleUppgiftDone från framework ([daa9b](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/daa9b20c869e617) Ulf Slunga)  
-  oulResponse/Status från rimfrost-framework-regel-manuell ([08f7b](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/08f7b67f0c64b2d) Ulf Slunga)  
-  använder regelRequestHandler från framework ([c1284](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/c12845b71d77414) Ulf Slunga)  
-  Use OulController from rimfrost-framework-oul ([ff3b2](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/ff3b2245ea136ca) Lars Persson)  
-  tar bort kafka regel dto som finns i ramverket ([21f63](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/21f6327368458b7) Ulf Slunga)  
-  Använder kundbehovsflöde dto's från framework ([bb15b](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/bb15bca8344f13c) Ulf Slunga)  
-  deserializer path ([de8b3](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/de8b3921bf1e595) Ulf Slunga)  

### Other changes

**raderar bortkommenterade dependencies**


[7d6f7](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/7d6f723cf2e5991) Ulf Slunga *2026-02-03 09:07:20*

**tar bort onödiga debug-utskrifter**


[7960a](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/7960ad9b6ac86bc) Ulf Slunga *2026-02-03 09:05:44*

**removing kafka that is in framework**

* tar bort oanvända metoder &amp; typer. 
* cleanup 
* feature: added health chećk feature for pom.xml 
* ix: Hantering av config.yaml 
* test: make quarkus tests run without kafka 

[42f75](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/42f759e3f3779d7) Julia Olsson Ductus *2026-02-03 07:03:59*


## 0.0.1 (2026-01-07)

### Other changes

**Apply spotless formatting**


[7b97d](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/7b97d01c5a7797d) Julia Olsson Ductus *2026-01-07 08:23:53*

**changed file rights**


[40ae9](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/40ae9adfa8f728a) Julia Olsson Ductus *2026-01-07 07:48:45*

**Update pom.xml**

* Co-authored-by: Ulf Slunga &lt;98820233+UlfSlunga-Sinetiq@users.noreply.github.com&gt; 

[b0948](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/b09484a8f052f3e) Julia Olsson Ductus *2025-12-18 12:49:25*

**Initial commit**


[30ec9](https://github.com/Forsakringskassan/rimfrost-regel-bekraftabeslut/commit/30ec96a97f1eb45) Julia Olsson Ductus *2025-12-15 15:23:23*


