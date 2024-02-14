<script lang="ts">
  import rq from '$lib/rq/rq.svelte';
  import type { components } from '$lib/types/api/v1/schema';
  import PostCommentEditModal from './PostCommentEditModal.svelte';

  const { post } = $props<{ post: components['schemas']['PostWithBodyDto'] }>();
  const title = `글 "${post.title}" 에 대한 댓글`;
  let postComment = $state<components['schemas']['PostCommentDto'] | undefined>();
  let postCommentEditModal = $state() as any;

  async function loadPostComment() {
    const { data: tempRsData } = await rq
      .apiEndPoints()
      .POST('/api/v1/postComments/{postId}/temp', {
        params: {
          path: {
            postId: post.id
          }
        }
      });

    postComment = tempRsData!.data.item;
  }

  async function save(
    post: components['schemas']['PostWithBodyDto'],
    postComment: components['schemas']['PostCommentDto'],
    body: string
  ) {
    const { data } = await rq.apiEndPoints().PUT('/api/v1/postComments/{postId}/{postCommentId}', {
      params: {
        path: {
          postId: post.id,
          postCommentId: postComment.id
        }
      },
      body: {
        body
      }
    });

    return data!.msg;
  }

  async function startWrite() {
    await loadPostComment();

    postCommentEditModal.showModal();
  }
</script>

<h2>댓글 작성</h2>

<button class="link" onclick={startWrite}>댓글 작성하기</button>

{#if postComment}
  <PostCommentEditModal bind:this={postCommentEditModal} {post} {postComment} {title} {save} />
{/if}
