<template>
  <div class="row justify-content-center">
    <div class="col-8">
      <main>
        <b-container class="py-5">
          <!-- The main card, rewritten with <b-card> -->
          <b-card class="p-3">
            <h1 class="display-4 text-dark mb-4">¡Gracias por enviar su información!</h1>
            <p class="lead mb-4">
              Hemos recibido su solicitud y la estamos procesando. A continuación, encontrará información importante sobre cómo puede hacer
              seguimiento a su estado.
            </p>

            <!-- The alert box, rewritten with <b-alert> -->
            <b-alert show variant="warning" class="mb-5">
              <div class="d-flex align-items-center">
                <font-awesome-icon icon="triangle-exclamation" class="mr-3 h4 mb-0" />
                <div>
                  <h4 class="alert-heading">¡Importante! Guarde este enlace.</h4>
                  <p>
                    Utilice el siguiente enlace para verificar el estado de su solicitud en cualquier momento. Es crucial que lo guarde en
                    un lugar seguro, ya que no podremos recuperarlo por usted.
                  </p>
                </div>
              </div>
            </b-alert>

            <div class="mb-4">
              <p class="mb-2">Puede seguir el estado de su solicitud accediendo al siguiente enlace:</p>
              <div class="bg-light p-3 rounded border">
                <div class="d-flex align-items-center justify-content-between">
                  <a v-if="trackingUrl" :href="trackingUrl" target="_blank" rel="noopener noreferrer" class="font-weight-bold text-break">
                    {{ trackingUrl }}
                  </a>
                  <b-button variant="secondary" @click="copyLink" title="Copiar enlace" class="ml-3 flex-shrink-0">
                    <font-awesome-icon icon="copy" />
                  </b-button>
                </div>
              </div>
              <p v-if="isCopied" class="text-success mt-2 small">¡Enlace copiado al portapapeles!</p>
            </div>

            <p class="text-muted">
              Si tiene alguna pregunta o necesita asistencia adicional, no dude en ponerse en contacto con nuestro equipo de soporte.
            </p>
          </b-card>
        </b-container>
      </main>
    </div>
  </div>
</template>
<script lang="ts" src="./public-pqrs-verify.component.ts"></script>
