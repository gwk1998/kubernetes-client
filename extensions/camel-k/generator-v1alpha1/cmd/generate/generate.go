/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main

import (
	"fmt"
	"reflect"

	v1 "github.com/apache/camel-k/pkg/apis/camel/v1"
	"github.com/apache/camel-k/pkg/apis/camel/v1alpha1"
	"github.com/fabric8io/kubernetes-client/generator/pkg/schemagen"
	machinery "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"knative.dev/pkg/apis"
)

func main() {

	// the CRD List types for which the model should be generated
	// no other types need to be defined as they are auto discovered
	crdLists := map[reflect.Type]schemagen.CrdScope{
		// v1alpha1
		reflect.TypeOf(v1alpha1.KameletList{}):        schemagen.Namespaced,
		reflect.TypeOf(v1alpha1.KameletBindingList{}): schemagen.Namespaced,
	}

	// constraints and patterns for fields
	constraints := map[reflect.Type]map[string]*schemagen.Constraint{
		//		reflect.TypeOf(v1alpha1.Step{}): {"Name": &schemagen.Constraint{MaxLength: 63, Pattern: "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$"}},
	}

	// types that are manually defined in the model
	providedTypes := []schemagen.ProvidedType{
		{GoType: reflect.TypeOf(v1.Flow{}), JavaClass: "com.fasterxml.jackson.databind.JsonNode"},
		{GoType: reflect.TypeOf(v1.TraitConfiguration{}), JavaClass: "com.fasterxml.jackson.databind.JsonNode"},
	}

	// go packages that are provided and where no generation is required and their corresponding java package
	providedPackages := map[string]string{
		// external
		"k8s.io/api/core/v1":                          "io.fabric8.kubernetes.api.model",
		"k8s.io/apimachinery/pkg/apis/meta/v1":        "io.fabric8.kubernetes.api.model",
		"k8s.io/apimachinery/pkg/api/resource":        "io.fabric8.kubernetes.api.model",
		"k8s.io/apimachinery/pkg/runtime":             "io.fabric8.kubernetes.api.model.runtime",
		"github.com/apache/camel-k/pkg/apis/camel/v1": "io.fabric8.camelk.v1",
	}

	// mapping of go packages of this module to the resulting java package
	// optional ApiGroup and ApiVersion for the go package (which is added to the generated java class)
	packageMapping := map[string]schemagen.PackageInformation{
		// v1alpha1
		"github.com/apache/camel-k/pkg/apis/camel/v1alpha1": {JavaPackage: "io.fabric8.camelk.v1alpha1", ApiGroup: "camel.apache.org", ApiVersion: "v1alpha1"},
	}

	// converts all packages starting with <key> to a java package using an automated scheme:
	//  - replace <key> with <value> aka "package prefix"
	//  - replace '/' with '.' for a valid java package name
	// e.g. github.com/apache/camel-k/pkg/apis/camel/v1/knative/CamelEnvironment is mapped to "io.fabric8.camelk.internal.pkg.apis.camel.v1.knative.CamelEnvironment"
	mappingSchema := map[string]string{
		//		"github.com/apache/camel-k/pkg/apis/camel/v1/knative": "io.fabric8.camelk.v1beta1.internal",
	}

	// overwriting some times
	manualTypeMap := map[reflect.Type]string{
		reflect.TypeOf(machinery.Time{}):       "java.lang.String",
		reflect.TypeOf(apis.URL{}):             "java.lang.String",
		reflect.TypeOf(apis.VolatileTime{}):    "java.lang.String",
		reflect.TypeOf(runtime.RawExtension{}): "java.util.Map<String, Object>",
		reflect.TypeOf(v1.Template{}):          "java.util.Map<String, Object>",
		reflect.TypeOf(v1alpha1.JSON{}):        "com.fasterxml.jackson.databind.JsonNode",
		reflect.TypeOf(v1alpha1.EndpointProperties{}): "java.util.Map<String, Object>",
	}

	json := schemagen.GenerateSchema("http://fabric8.io/camel-k/v1alpha1/CamelKSchema#", crdLists, providedPackages, manualTypeMap, packageMapping, mappingSchema, providedTypes, constraints, "io.fabric8")

	fmt.Println(json)
}
