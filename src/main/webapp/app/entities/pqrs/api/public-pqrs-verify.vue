<template>
  <div class="row justify-content-center">
    <div class="col-lg-8 col-md-10">
      <main>
        <b-container class="py-5">
          <b-card class="p-3">
            <h1 class="display-4 text-dark mb-4" v-text="t$('ventanillaUnicaApp.pqrs.verify.title')"></h1>

            <p class="lead mb-4" v-text="t$('ventanillaUnicaApp.pqrs.verify.intro')"></p>

            <b-alert show variant="warning" class="mb-5">
              <div class="d-flex align-items-center">
                <font-awesome-icon icon="triangle-exclamation" class="mr-3 h4 mb-0" />
                <div>
                  <h4 class="alert-heading" v-text="t$('ventanillaUnicaApp.pqrs.verify.alert.title')"></h4>
                  <p v-text="t$('ventanillaUnicaApp.pqrs.verify.alert.body')"></p>
                </div>
              </div>
            </b-alert>

            <div class="mb-4">
              <p class="mb-2" v-text="t$('ventanillaUnicaApp.pqrs.verify.link.description')"></p>
              <div class="bg-light p-3 rounded border">
                <div class="d-flex align-items-center justify-content-between">
                  <a v-if="trackingUrl" :href="trackingUrl" target="_blank" rel="noopener noreferrer" class="font-weight-bold text-break">
                    {{ trackingUrl }}
                  </a>
                  <b-button
                    variant="secondary"
                    @click="copyLink"
                    :title="t$('ventanillaUnicaApp.pqrs.verify.link.copyTitle')"
                    class="ml-3 flex-shrink-0"
                  >
                    <font-awesome-icon icon="copy" />
                  </b-button>
                </div>
              </div>
              <p v-if="isCopied" class="text-success mt-2 small" v-text="t$('ventanillaUnicaApp.pqrs.verify.link.copiedMessage')"></p>
            </div>

            <p class="text-muted" v-text="t$('ventanillaUnicaApp.pqrs.verify.footer')"></p>
          </b-card>
        </b-container>
      </main>
    </div>
  </div>
</template>

<script lang="ts" src="./public-pqrs-verify.component.ts"></script>
