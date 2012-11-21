
// such as "stack" as in "news-1.stack.aws-dev.barchart.com"
def ParamInfix = project.properties["ParamInfix"]

// such as "news.aws-dev.barchart.com" from barchart-configuration
def ParamIdentity = project.properties["ParamIdentity"]

// dns zone hosting identity record
def ParamZoneName = project.properties["ParamZoneName"]

// list of dns records from same dns zone as identity record
def ParamNameList = project.properties["ParamNameList"]

//////////////////

def nameList = ParamNameList.split(";")

def stackPrefix = ParamIdentity.replaceAll("." + ParamZoneName, "")
def stackSuffix = ParamInfix + "." + ParamZoneName

def numberList = [] 

for( name in nameList ){
	if( name.startsWith(stackPrefix) && name.endsWith(stackSuffix) ){
		numeric = name.replaceAll(stackPrefix,"").replaceAll(stackSuffix,"").replaceAll("\\D","")
		number = (Long) numeric
		numberList.add( number )
	}
}

project.properties["ParamHostName"] = ParamHostName
