<script lang="ts">
  import PostDetail from '$lib/business/post/PostDetail.svelte';
  import PostCommentWriteAndList from '$lib/business/postComment/PostCommentWriteAndList.svelte';
</script>

<div class="flex-grow flex justify-center items-center">
  <div class="w-full px-2 mt-4">
    <PostDetail />
    <PostCommentWriteAndList />
  </div>
</div>
