/**
 * barchart cloud formation stack naming rules:
 * 
 * find existing, if any, and increment stack index
 */

//////////////////

/**
 * such as "stack" as in "news-1.stack.aws-dev.barchart.com"
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

//////////////////

def nameList = ParamNameList.split(";")

def stackPrefix = ParamIdentity.replaceAll("." + ParamZoneName, "")
def stackSuffix = ParamInfix + "." + ParamZoneName

def numberList = [];

for( name in nameList ){
	if( name.startsWith(stackPrefix) && name.endsWith(stackSuffix) ){

		def index = name
				.replaceFirst(stackPrefix,"")
				.replaceAll(stackSuffix,"")
				.replaceAll("\\D","")

		def number = index.toLong()

		numberList.add( number )

	}
}

def number = numberList.max() + 1

def ParamHostName = "$stackPrefix-$number.$stackSuffix"

println "### ParamHostName : $ParamHostName"

/**
 * cloud formation uses strictly formatted dns names
 */
project.properties["ParamZoneName"] = ParamZoneName + "."
project.properties["ParamHostName"] = ParamHostName + "."
