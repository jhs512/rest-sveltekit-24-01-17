<script lang="ts">
  import rq from '$lib/rq/rq.svelte';
  import type { components } from '$lib/types/api/v1/schema';

  const { post } = $props<{ post: components['schemas']['PostWithBodyDto'] }>();

  function showModal() {
    const modal = rq.showModal('post_comment_write_modal');

    const inputBody = modal.querySelector('form input[type=text]') as HTMLInputElement;

    inputBody.focus();
  }

  function hideModal() {
    rq.hideModal('post_comment_write_modal');
  }

  async function submitWriteForm(this: HTMLFormElement) {
    const form = this;

    form.body.value = form.body.value.trim();

    if (form.body.value.length == 0) {
      rq.msgError('댓글 내용을 입력해주세요.');
      form.body.focus();

      return;
    }

    const { data: tempRsData } = await rq
      .apiEndPoints()
      .POST('/api/v1/postComments/{postId}/temp', {
        params: {
          path: {
            postId: post.id
          }
        }
      });

    const postCommentId = tempRsData!.data.item.id;

    const { data: putRsData } = await rq
      .apiEndPoints()
      .PUT('/api/v1/postComments/{postId}/{postCommentId}', {
        params: {
          path: {
            postId: post.id,
            postCommentId
          }
        },
        body: {
          body: form.body.value
        }
      });

    form.body.value = '';

    hideModal();

    rq.msgInfo(putRsData!.msg);
  }
</script>

<h2>댓글 작성</h2>

<button class="link" onclick={showModal}>댓글 작성하기</button>
<dialog id="post_comment_write_modal" class="modal" style="z-index: 100 !important;">
  <div class="modal-box">
    <form method="dialog">
      <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">✕</button>
    </form>
    <h3 class="font-bold text-lg">글 "{post.title}" 에 대한 댓글</h3>

    <form class="grid gap-4" on:submit|preventDefault={submitWriteForm}>
      <label class="form-control">
        <div class="label">
          <span class="label-text">댓글 내용</span>
        </div>
        <input
          type="text"
          name="body"
          placeholder="댓글을 입력해주세요."
          class="input input-bordered w-full"
        />
      </label>

      <div class="grid grid-cols-2 gap-4">
        <button class="btn btn-outline" type="button" onclick={hideModal}>취소</button>
        <button class="btn btn-primary" type="submit">댓글 작성</button>
      </div>
    </form>
  </div>
  <form method="dialog" class="modal-backdrop">
    <button>close</button>
  </form>
</dialog>
