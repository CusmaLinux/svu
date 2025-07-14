<template>
  <div id="app" class="app-wrapper">
    <ribbon></ribbon>
    <div id="app-header">
      <jhi-navbar></jhi-navbar>
    </div>
    <div class="container-fluid">
      <main class="main-content">
        <div class="card jh-card">
          <router-view></router-view>
        </div>
        <b-modal id="login-page" hide-footer lazy>
          <template #modal-title>
            <span data-cy="loginTitle" id="login-title" v-text="t$('login.title')"></span>
          </template>
          <login-form></login-form>
        </b-modal>
      </main>
      <svu-auth-footer v-if="authenticated"></svu-auth-footer>
      <svu-footer v-else></svu-footer>
    </div>
    <b-toaster name="b-toaster-top-right" class="p-3"></b-toaster>
  </div>
</template>

<script lang="ts" src="./app.component.ts"></script>

<style>
html,
body {
  height: 100%;
  margin: 0;
}
.app-wrapper {
  display: flex;
  flex-direction: column;
}
.main-content {
  flex-grow: 1;
}
</style>
