# rimfrost-regel-bekraftabeslut changelog

Changelog of rimfrost-regel-bekraftabeslut.

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


