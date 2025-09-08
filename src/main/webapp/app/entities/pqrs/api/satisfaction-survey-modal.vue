<template>
  <div>
    <div class="likert-scale-info">
      <p>La siguiente encuesta se basa en la escala de Likert, donde 1 es "totalmente insatisfecho" y 5 es "totalmente satisfecho".</p>
    </div>
    <form>
      <div class="form-group">
        <label for="rating">Calificación:</label>
        <div class="star-rating">
          <span v-for="star in 5" :key="star" class="star" :class="{ filled: star <= rating }" @click="setRating(star)"> &#9733; </span>
        </div>
      </div>
      <div class="form-group">
        <label for="comment">Comentario:</label>
        <textarea class="form-control" id="comment" :value="comment" @input="handleCommentInput"></textarea>
      </div>
    </form>
  </div>
</template>

<script lang="ts" src="./satisfaction-survey-modal.component.ts"></script>

<style scoped>
.star-rating {
  font-size: 2rem;
  cursor: pointer;
}

.star {
  color: #ccc;
}

.star.filled {
  color: #ffc107;
}

.likert-scale-info {
  margin-bottom: 1rem;
}
</style>
