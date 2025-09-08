import { defineComponent } from 'vue';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'SatisfactionSurveyModal',
  props: {
    rating: {
      type: Number,
      default: null,
    },
    comment: {
      type: String,
      default: '',
    },
  },
  emits: ['update:rating', 'update:comment'],
  setup(props, { emit }) {
    const setRating = (rating: number) => {
      emit('update:rating', rating);
    };

    const handleCommentInput = (event: Event) => {
      emit('update:comment', (event.target as HTMLTextAreaElement).value);
    };

    return {
      setRating,
      handleCommentInput,
    };
  },
});
