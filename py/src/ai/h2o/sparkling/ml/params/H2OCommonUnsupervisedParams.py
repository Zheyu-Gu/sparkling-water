#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from pyspark.ml.param import *

from ai.h2o.sparkling.ml.params.H2OCommonParams import H2OCommonParams
from ai.h2o.sparkling.ml.params.H2OTypeConverters import H2OTypeConverters


class H2OCommonUnsupervisedParams(H2OCommonParams):
    foldCol = Param(
        Params._dummy(),
        "foldCol",
        "Fold column name",
        H2OTypeConverters.toNullableString())

    weightCol = Param(
        Params._dummy(),
        "weightCol",
        "Weight column name",
        H2OTypeConverters.toNullableString())

    nfolds = Param(
        Params._dummy(),
        "nfolds",
        "Number of fold columns",
        H2OTypeConverters.toInt())

    ##
    # Getters
    ##
    def getFoldCol(self):
        return self.getOrDefault(self.foldCol)

    def getWeightCol(self):
        return self.getOrDefault(self.weightCol)

    def getNfolds(self):
        return self.getOrDefault(self.nfolds)

    ##
    # Setters
    ##
    def setFoldCol(self, value):
        return self._set(foldCol=value)

    def setWeightCol(self, value):
        return self._set(weightCol=value)

    def setNfolds(self, value):
        return self._set(nfolds=value)
