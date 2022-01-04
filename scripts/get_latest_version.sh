#!/bin/bash
#
# Copyright 2010-2022 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


targetArtifact=$1
targetMinorVersion=$2
majorVersion=${targetMinorVersion%.*}
minorVersion=${targetMinorVersion#*.}
while read -r line; do
  maintenanceVersion=${line#<a*>${targetMinorVersion}.} && maintenanceVersion=${maintenanceVersion%%.*}
  maintenanceVersions="${maintenanceVersions}${maintenanceVersion}"$'\n'
done<<END
  $(curl -s "https://repo1.maven.org/maven2/org/springframework/${targetArtifact}/" | grep "RELEASE" | grep -E ">${majorVersion}\.${minorVersion}\.[0-9]*")
END
echo "${targetMinorVersion}.$(echo "${maintenanceVersions}" | sort -n | tail -n 1).RELEASE"
