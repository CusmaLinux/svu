<template>
  <div class="d-flex flex-grow-1">
    <main class="flex-grow-1 p-4 p-md-5">
      <b-container>
        <!-- "Introduction Section -->
        <section class="mb-5 text-center">
          <h1 class="display-4 fw-bold text-dark mb-4">Sistema de Ventanilla Única (SVU)</h1>
          <div class="col-lg-8 mx-auto">
            <p class="lead text-muted">
              Bienvenido al Sistema de Ventanilla Única del Instituto Tecnológico del Putumayo. Aquí puede gestionar sus Peticiones, Quejas,
              Reclamos, Sugerencias y Demandas (PQRSD) de forma eficiente y transparente.
            </p>
          </div>
          <div class="mt-5 d-flex justify-content-center gap-3">
            <b-button variant="primary" size="lg" to="/public/pqrs/new" class="d-flex align-items-center br-lg">
              <font-awesome-icon icon="plus-circle" class="me-2" />
              Crear PQRSD
            </b-button>
            <b-button variant="secondary" size="lg" to="/public/pqrs/consult" class="d-flex align-items-center br-lg">
              <font-awesome-icon icon="search" class="me-2" />
              Consultar PQRSD
            </b-button>
          </div>
        </section>

        <!-- "What is SVU?" Section -->
        <section class="mb-5">
          <b-row class="align-items-center g-5">
            <b-col md="6">
              <b-card class="card">
                <h2 class="h2 fw-semibold text-dark mb-4">¿Qué es el SVU?</h2>
                <p class="text-muted mb-3">
                  El Sistema de Ventanilla Única (SVU) es una plataforma diseñada para optimizar la gestión de PQRSD en el Instituto
                  Tecnológico del Putumayo (ITP). Nuestro objetivo es proporcionar un canal centralizado y eficiente para que la comunidad
                  educativa pueda interactuar con la institución.
                </p>
                <p class="text-muted">
                  Este sistema automatiza el proceso de gestión, mejorando la eficiencia operativa, garantizando el cumplimiento de plazos y
                  ofreciendo respuestas claras y rápidas. Buscamos fortalecer la confianza de la comunidad educativa y contribuir a la
                  modernización de los procesos administrativos en la región.
                </p>
              </b-card>
            </b-col>
            <b-col md="6" class="text-center">
              <img
                alt="Ilustración de personas gestionando PQRSD"
                class="img-fluid shadow custom-rounded-lg"
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuDEly6EX-ETjivrMkNqCUWrD2bgigrLJBjIbqhD_cjP94i_Fxp7YFxTFXgSjhaG7tdIFileN2mCD6r6SkihxN5_se4KYqweKwFhzLtE-DNdP3L_PojuiKHe6FemYJemacpcOMBRmrYe7lRq8H8S2GG-v9ugpuiC0VkHpn2CUyQQYTma0yFmnYAmzq7_Z2TJVag-8hM9x3EyKu9tZ2Unkxae9KsJeIRPz8HuGzbd764cDRVmFzOvVrV592rL0xtWkOI87Fh4Iv7jQg"
              />
            </b-col>
          </b-row>
        </section>

        <!-- Features Section -->
        <section class="mb-5">
          <h2 class="h2 fw-semibold text-dark mb-5 text-center">Características del Sistema</h2>
          <b-row class="g-4">
            <b-col md="4">
              <b-card class="card text-center h-100 d-flex flex-column justify-content-center">
                <font-awesome-icon icon="id-card" class="text-primary mb-3" size="3x" />
                <h3 class="h5 fw-medium text-dark mb-2">Radicación Anónima</h3>
                <p class="text-muted">
                  Radique sus requerimientos (PQRSD) de forma anónima, con la opción de adjuntar documentos relevantes.
                </p>
              </b-card>
            </b-col>
            <b-col md="4">
              <b-card class="card text-center h-100 d-flex flex-column justify-content-center">
                <font-awesome-icon icon="folder-open" class="text-primary mb-3" size="3x" />
                <h3 class="h5 fw-medium text-dark mb-2">Consulta de Requerimientos</h3>
                <p class="text-muted">
                  Consulte el estado de su requerimiento utilizando el número de radicado o un enlace con token de acceso.
                </p>
              </b-card>
            </b-col>
            <b-col md="4">
              <b-card class="card text-center h-100 d-flex flex-column justify-content-center">
                <font-awesome-icon icon="comments" class="text-primary mb-3" size="3x" />
                <h3 class="h5 fw-medium text-dark mb-2">Comunicación Directa</h3>
                <p class="text-muted">
                  Responda a las comunicaciones de los funcionarios y adjunte archivos si es necesario, manteniendo un canal de diálogo
                  abierto.
                </p>
              </b-card>
            </b-col>
          </b-row>
        </section>

        <!-- FAQ Section -->
        <section class="mb-5">
          <h2 class="h2 fw-semibold text-dark mb-5 text-center">Preguntas Frecuentes (FAQ)</h2>
          <div class="col-lg-8 mx-auto">
            <b-accordion :items="faqItems"></b-accordion>
          </div>
        </section>
      </b-container>
    </main>

    <!-- Mobile action buttons -->
    <div class="d-md-none fixed-bottom bg-dark p-3 shadow-lg d-flex justify-content-around">
      <router-link to="/public/pqrs/new" href="#" class="d-flex flex-column align-items-center text-light text-decoration-none">
        <font-awesome-icon icon="plus-circle" size="lg" />
        <span class="small mt-1">Crear</span>
      </router-link>
      <router-link to="/public/pqrs/consult" class="d-flex flex-column align-items-center text-light text-decoration-none">
        <font-awesome-icon icon="search" size="lg" />
        <span class="small mt-1">Consultar</span>
      </router-link>
    </div>
  </div>
</template>

<script src="./welcome.component.ts" ang="ts"></script>

<style>
.fixed-bottom a:hover {
  color: var(--bs-primary) !important;
}

.card {
  background-color: rgb(255 255 255);
  padding: 1.5rem;
  border-radius: 0.75rem;
  box-shadow:
    0 10px 15px -3px rgba(0, 0, 0, 0.1),
    0 4px 6px -4px rgba(0, 0, 0, 0.1);
}

.custom-rounded-lg {
  border-radius: 1.15rem;
}
</style>
