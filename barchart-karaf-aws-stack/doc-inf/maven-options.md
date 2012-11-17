clean package

--activate-profiles image-launch,stack-create

--define amazonRegion=${amazonRegion} 
--define stackName=${stackName}

--define ParamInstanceType=${ParamInstanceType}
--define ParamKeyName=${ParamKeyName}
--define ParamIdentity=${ParamIdentity}
--define ParamTopicName=${ParamTopicName}
--define ParamMinSize=${ParamMinSize}
--define ParamMaxSize=${ParamMaxSize}
--define ParamSecurityGroup=${ParamSecurityGroup}

