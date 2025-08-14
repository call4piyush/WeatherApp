{{/*
Expand the name of the chart.
*/}}
{{- define "weather-app.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "weather-app.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "weather-app.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "weather-app.labels" -}}
helm.sh/chart: {{ include "weather-app.chart" . }}
{{ include "weather-app.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: weather-app
{{- end }}

{{/*
Selector labels
*/}}
{{- define "weather-app.selectorLabels" -}}
app.kubernetes.io/name: {{ include "weather-app.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "weather-app.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "weather-app.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the secret to use
*/}}
{{- define "weather-app.secretName" -}}
{{- printf "%s-secrets" (include "weather-app.fullname" .) }}
{{- end }}

{{/*
Create the name of the configmap to use
*/}}
{{- define "weather-app.configMapName" -}}
{{- printf "%s-config" (include "weather-app.fullname" .) }}
{{- end }}

{{/*
Database host
*/}}
{{- define "weather-app.databaseHost" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "%s-postgresql" (include "weather-app.fullname" .) }}
{{- else if .Values.externalDatabase.enabled }}
{{- .Values.externalDatabase.host }}
{{- else }}
{{- printf "%s-postgresql" (include "weather-app.fullname" .) }}
{{- end }}
{{- end }}

{{/*
Database port
*/}}
{{- define "weather-app.databasePort" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.primary.service.ports.postgresql | default 5432 }}
{{- else if .Values.externalDatabase.enabled }}
{{- .Values.externalDatabase.port }}
{{- else }}
{{- 5432 }}
{{- end }}
{{- end }}

{{/*
Database name
*/}}
{{- define "weather-app.databaseName" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.auth.database }}
{{- else if .Values.externalDatabase.enabled }}
{{- .Values.externalDatabase.database }}
{{- else }}
{{- "weatherdb" }}
{{- end }}
{{- end }}

{{/*
Database username
*/}}
{{- define "weather-app.databaseUsername" -}}
{{- if .Values.postgresql.enabled }}
{{- .Values.postgresql.auth.username }}
{{- else if .Values.externalDatabase.enabled }}
{{- .Values.externalDatabase.username }}
{{- else }}
{{- "weatheruser" }}
{{- end }}
{{- end }}

{{/*
Database password secret name
*/}}
{{- define "weather-app.databaseSecretName" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "%s-postgresql" (include "weather-app.fullname" .) }}
{{- else if .Values.externalDatabase.existingSecret }}
{{- .Values.externalDatabase.existingSecret }}
{{- else }}
{{- include "weather-app.secretName" . }}
{{- end }}
{{- end }}

{{/*
Database password secret key
*/}}
{{- define "weather-app.databaseSecretKey" -}}
{{- if .Values.postgresql.enabled }}
{{- "password" }}
{{- else if .Values.externalDatabase.existingSecretPasswordKey }}
{{- .Values.externalDatabase.existingSecretPasswordKey }}
{{- else }}
{{- "database-password" }}
{{- end }}
{{- end }}

{{/*
Backend service name
*/}}
{{- define "weather-app.backend.serviceName" -}}
{{- printf "%s-backend" (include "weather-app.fullname" .) }}
{{- end }}

{{/*
Frontend service name
*/}}
{{- define "weather-app.frontend.serviceName" -}}
{{- printf "%s-frontend" (include "weather-app.fullname" .) }}
{{- end }}

{{/*
Backend ingress hostname
*/}}
{{- define "weather-app.backend.hostname" -}}
{{- if .Values.backend.ingress.hosts }}
{{- (index .Values.backend.ingress.hosts 0).host }}
{{- else }}
{{- printf "api.%s" .Values.global.domain | default "example.com" }}
{{- end }}
{{- end }}

{{/*
Frontend ingress hostname
*/}}
{{- define "weather-app.frontend.hostname" -}}
{{- if .Values.frontend.ingress.hosts }}
{{- (index .Values.frontend.ingress.hosts 0).host }}
{{- else }}
{{- .Values.global.domain | default "example.com" }}
{{- end }}
{{- end }}

{{/*
Create image pull secret name
*/}}
{{- define "weather-app.imagePullSecretName" -}}
{{- printf "%s-registry-secret" (include "weather-app.fullname" .) }}
{{- end }}

{{/*
Validate required values
*/}}
{{- define "weather-app.validateValues" -}}
{{- if not .Values.secrets.openweatherApiKey }}
{{- fail "OpenWeatherMap API key is required. Please set secrets.openweatherApiKey" }}
{{- end }}
{{- if and .Values.externalDatabase.enabled (not .Values.externalDatabase.host) }}
{{- fail "External database host is required when externalDatabase.enabled is true" }}
{{- end }}
{{- end }}

{{/*
Common environment variables for backend
*/}}
{{- define "weather-app.backend.commonEnv" -}}
- name: SPRING_PROFILES_ACTIVE
  value: {{ .Values.global.environment | default "kubernetes" | quote }}
- name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
  value: "health,info,metrics,prometheus"
- name: MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS
  value: "always"
- name: LOGGING_LEVEL_COM_WEATHERAPP
  value: "INFO"
{{- end }}

{{/*
Common environment variables for frontend
*/}}
{{- define "weather-app.frontend.commonEnv" -}}
- name: API_URL
  value: "http://{{ include "weather-app.backend.serviceName" . }}:{{ .Values.backend.service.port }}"
- name: ENVIRONMENT
  value: {{ .Values.global.environment | default "kubernetes" | quote }}
{{- end }} 