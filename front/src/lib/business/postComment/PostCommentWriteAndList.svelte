<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import rq from '$lib/rq/rq.svelte';
  import type { components } from '$lib/types/api/v1/schema';

  let post = $state() as components['schemas']['PostWithBodyDto'];

  async function loadPost() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq
      .apiEndPoints()
      .GET('/api/v1/posts/{id}', { params: { path: { id: parseInt($page.params.id) } } });

    if (error) throw error;

    post = data!.data.item;

    return data!;
  }

  onMount(() => {
    loadPost();
  });
</script>

<h1>댓글</h1>

{#if post}
  <div><span class="text-gray-400">{post.title}</span> 에 대한 댓글</div>

  <h2>댓글 작성</h2>
  <h2>댓글 목록</h2>
{/if}
