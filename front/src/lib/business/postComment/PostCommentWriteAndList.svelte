<script lang="ts">
  import type { components } from '$lib/types/api/v1/schema';

  const { post } = $props<{ post: components['schemas']['PostWithBodyDto'] }>();
</script>

<h1>댓글</h1>

<div><span class="text-gray-400">{post.title}</span> 에 대한 댓글</div>

<h2>댓글 작성</h2>
<h2>댓글 목록</h2>
