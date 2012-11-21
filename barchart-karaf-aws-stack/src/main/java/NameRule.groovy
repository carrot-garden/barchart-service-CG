/**
 * barchart cloud formation stack naming convention:
 * 
 * pattern: [prefix]-[index].[infix].[suffix]
 * 
 * prefix = first part of identity dns name
 * index = auto increment
 * infix = stack middle name
 * suffix = last part of identity dns name
 * 
 * find existing, if any, and increment stack index
 */

//////////////////

/**
 * such as "stack" as in "news-1.stack.aws-dev.barchart.com."
 */ 
def ParamInfix = project.properties["ParamInfix"]

/**
 * such as "news.aws-dev.barchart.com" from https://github.com/barchart/barchart-configuration
 */ 
def ParamIdentity = project.properties["ParamIdentity"]

/**
 * dns zone hosting identity record
 */ 
def ParamZoneName = project.properties["ParamZoneName"]

/**
 * list of dns records from same dns zone as identity record
 */ 
def ParamNameList = project.properties["ParamNameList"]

/**
 * calculated parameters result destination file
 */
def templateParamFile = project.properties["templateParamFile"] 

//////////////////

/** stack name constraint */
def amazonFilter( name ) {
	name.replaceAll("[^A-Za-z0-9-]","-")
}

def hostPrefix = (ParamIdentity + ".").replaceAll("." + ParamZoneName, "")

def stackPrefix = amazonFilter(hostPrefix) 
def stackSuffix = ParamInfix + "." + ParamZoneName

def nameList = ParamNameList.split(";")

def indexList = [ 0 ];

for ( name in nameList ) {
	if( name.startsWith(stackPrefix) && name.endsWith(stackSuffix) ){

		def index = "0" + name
				.replaceFirst(stackPrefix,"")
				.replaceAll(stackSuffix,"")
				.replaceAll("\\D","")
				
		indexList.add( index.toLong() )

	}
}

def stackIndex = indexList.max() + 1

/** final result */
def stackName = "$stackPrefix-$stackIndex"
def ParamHostName = "$stackName.$stackSuffix"

/** report on console */
println "### stackName : $stackName"
println "### ParamZoneName : $ParamZoneName"
println "### ParamHostName : $ParamHostName"

/** store in pom */
project.properties["stackName"] = stackName
project.properties["ParamZoneName"] = ParamZoneName
project.properties["ParamHostName"] = ParamHostName

/** store in file */
def timestamp = new Date()
new File(templateParamFile).write(
"""
#
# auto-generated on $timestamp
#

stackName=$stackName

ParamZoneName=$ParamZoneName
ParamHostName=$ParamHostName

"""
)
