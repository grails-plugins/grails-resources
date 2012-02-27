#!/bin/sh
VERSION=$1
ZIP=../resources-$VERSION.zip
echo "<plugin name='resources' version='$VERSION' grailsVersion='1.3 &gt; *'>
  <author>Marc Palmer, Luke Daley</author>
  <authorEmail>marc@grailsrocks.com, ld@ldaley.com</authorEmail>
  <title>Resources</title>
  <description>HTML resource management enhancements to replace g.resource etc.</description>
  <documentation>http://grails.org/plugin/resources</documentation>
  <resources>
    <resource>ResourcesBootStrap</resource>
    <resource>WebXmlConfig</resource>
    <resource>org.grails.plugin.resource.BundleResourceMapper</resource>
    <resource>org.grails.plugin.resource.CSSPreprocessorResourceMapper</resource>
    <resource>org.grails.plugin.resource.CSSRewriterResourceMapper</resource>
    <resource>org.grails.plugin.resource.ResourceTagLib</resource>
  </resources>
  <dependencies />
  <behavior />
</plugin>" >plugin.xml
rm $ZIP
zip -r $ZIP LICENSE.txt ResourcesGrailsPlugin.groovy application.properties grails-app/conf/ResourcesBootStrap.groovy grails-app/resourceMappers plugin.xml grails-app/taglib src
zip -d $ZIP grails-app/resourceMappers/org/grails/plugin/resource/test/TestResourceMapper.groovy
zip -d $ZIP grails-app/resourceMappers/org/grails/plugin/resource/test
rm plugin.xml
echo mvn deploy:deploy-file -Durl=file:///$HOME/.m2/repository -Dfile=$ZIP -Dpackaging=zip -DpomFile=pom.xml
mvn deploy:deploy-file -Durl=file:///$HOME/.m2/repository -Dfile=$ZIP -Dpackaging=zip -DpomFile=pom.xml
