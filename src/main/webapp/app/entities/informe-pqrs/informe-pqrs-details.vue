<template>
  <b-container class="flex-grow-1 container py-5">
    <b-row v-if="informePqrs">
      <b-col>
        <!-- The main card for the report -->
        <b-card no-body class="shadow-lg">
          <b-card-body class="p-4 p-lg-5">
            <div class="d-flex justify-content-between align-items-start mb-4">
              <div>
                <h1 class="h3 font-weight-bold text-dark mb-1">Informe Pqrs</h1>
                <p class="text-muted small">ID: {{ informePqrs.id }}</p>
              </div>
              <router-link
                v-if="informePqrs.id"
                :to="{ name: 'InformePqrsEdit', params: { informePqrsId: informePqrs.id } }"
                custom
                v-slot="{ navigate }"
              >
                <b-button @click="navigate" variant="link" class="text-muted p-0">
                  <font-awesome-icon icon="pen-to-square" size="2x"></font-awesome-icon>
                </b-button>
              </router-link>
            </div>

            <!-- Report Details Grid -->
            <b-row class="mb-5">
              <b-col cols="12" md="6" class="mb-4">
                <h2 class="h6 text-muted font-weight-bold mb-1">Fecha Inicio</h2>
                <p class="text-dark lead mb-0">{{ formatDateLong(informePqrs.fechaInicio) }}</p>
              </b-col>
              <b-col cols="12" md="6" class="mb-4">
                <h2 class="h6 text-muted font-weight-bold mb-1">Fecha Fin</h2>
                <p class="text-dark lead mb-0">{{ formatDateLong(informePqrs.fechaFin) }}</p>
              </b-col>
              <b-col cols="12" md="6" class="mb-4">
                <h2 class="h6 text-muted font-weight-bold mb-1">Total Pqrs</h2>
                <p class="text-dark lead font-weight-bold mb-0">{{ informePqrs.totalPqrs }}</p>
              </b-col>
              <b-col cols="12" md="6" class="mb-4">
                <h2 class="h6 text-muted font-weight-bold mb-1">Total Resueltas</h2>
                <p class="text-success lead font-weight-bold mb-0">{{ informePqrs.totalResueltas }}</p>
              </b-col>
              <b-col cols="12" md="6" class="mb-4">
                <h2 class="h6 text-muted font-weight-bold mb-1">Total Pendientes</h2>
                <p class="text-warning lead font-weight-bold mb-0">{{ informePqrs.totalPendientes }}</p>
              </b-col>
              <b-col cols="12" md="6" class="mb-4">
                <h2 class="h6 text-muted font-weight-bold mb-1">Oficina</h2>
                <div v-if="informePqrs.oficina">
                  <router-link class="text-primary lead" :to="{ name: 'OficinaView', params: { oficinaId: informePqrs.oficina.id } }">{{
                    informePqrs.oficina.nombre
                  }}</router-link>
                </div>
              </b-col>
            </b-row>

            <!-- Data Visualization / Chart Placeholder -->
            <div class="bg-light p-4 rounded text-left mb-5">
              <h2 class="h4 font-weight-bold text-dark mb-4">Visualización de Datos</h2>

              <div class="aspect-ratio-box rounded">
                <div class="aspect-ratio-box-inside d-flex align-items-center justify-content-center">
                  <v-chart v-if="informePqrs.id" class="chart" :option="chartOptions" autoresize />
                </div>
              </div>
            </div>

            <!-- Download Button -->
            <div class="d-flex text-center gap-3">
              <b-button
                type="submit"
                @click.prevent="previousState()"
                class="btn btn-info"
                data-cy="entityDetailsBackButton"
                variant="info"
                size="lg"
              >
                <font-awesome-icon icon="arrow-left"></font-awesome-icon>&nbsp;<span v-text="t$('entity.action.back')"></span>
              </b-button>

              <b-button v-if="informePqrs.id" variant="success" size="lg" @click="downloadReport(informePqrs.id)" :disabled="isDownloading">
                <font-awesome-icon icon="download" class="mr-2"></font-awesome-icon>
                <span v-if="!isDownloading">Descargar Reporte (.xlsx)</span>
                <span v-else>Descargando...</span>
              </b-button>
            </div>
          </b-card-body>
        </b-card>
      </b-col>
    </b-row>
  </b-container>
</template>

<script lang="ts" src="./informe-pqrs-details.component.ts"></script>

<style scoped>
.aspect-ratio-box {
  position: relative;
  width: 100%;
  height: 0;
  overflow: hidden; /* Ensures content doesn't spill out */
  /* 16:9 Aspect Ratio: (9 / 16) * 100% = 56.25% */
  padding-top: 56.25%;
}

.aspect-ratio-box-inside {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #e0e7ff;
}

.lead {
  font-size: 1.1rem;
}

.chart {
  width: 100%;
  height: 100%;
}
</style>
