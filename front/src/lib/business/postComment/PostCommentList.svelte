<script lang="ts">
  import type { components } from '$lib/types/api/v1/schema';

  const { post } = $props<{ post: components['schemas']['PostWithBodyDto'] }>();
</script>

<h2>댓글 목록</h2>
