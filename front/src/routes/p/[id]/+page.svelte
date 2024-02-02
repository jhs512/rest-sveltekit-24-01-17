<script lang="ts">
  import { page } from '$app/stores';
  import rq from '$lib/rq/rq.svelte';
  import hotkeys from 'hotkeys-js';
  import ToastUiEditor from '$lib/components/ToastUiEditor.svelte';
  import { prettyDateTime } from '$lib/utils';
  import type { components } from '$lib/types/api/v1/schema';

  const toastUiEditors = $state<Record<number, any>>({});

  let toastUiEditor = $state<any | undefined>();
  let writeCommentToastUiEditor = $state<any | undefined>();

  let tempPostCommentId = $state(0);

  async function loadPost() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq
      .apiEndPoints()
      .GET('/api/v1/posts/{id}', { params: { path: { id: parseInt($page.params.id) } } });

    if (error) throw error;

    Post__lastModified = data!.data.item.modifyDate;

    Post__loadLatestBody();

    return data!;
  }

  let Post__lastModified = '';

  async function Post__loadLatestBody() {
    // 에디터 자체가 아직 로드가 안되었다면 1초 후에 재시도
    if (toastUiEditor === undefined) {
      setTimeout(Post__loadLatestBody, 1000);
      return;
    }

    // 브라우저 자체가 숨겨져있거나, 뷰어가 풀스크린이 아니라면 5초 후에 재시도
    if (document.hidden || !toastUiEditor.isFullScreen) {
      setTimeout(Post__loadLatestBody, 5000);
      return;
    }

    const postId = $page.params.id;

    const { data, error } = await rq.apiEndPoints().GET('/api/v1/posts/{id}/body', {
      params: { path: { id: parseInt(postId) }, query: { lastModifyDate: Post__lastModified } }
    });

    if (data) {
      rq.msgInfo(data.msg);

      toastUiEditor.editor.setMarkdown(data.data.body);

      Post__lastModified = data.data.modifyDate;
    }

    setTimeout(Post__loadLatestBody, 5000);
  }

  let postComments = $state<components['schemas']['PostCommentDto'][]>([]);

  async function loadPostComments() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const { data, error } = await rq.apiEndPoints().GET('/api/v1/postComments/{postId}', {
      params: { path: { postId: parseInt($page.params.id) } }
    });

    if (error) throw error;

    postComments = data!.data.items;

    return data!;
  }

  async function confirmAndDeletePostComment(
    postComment: components['schemas']['PostCommentDto'],
    callback: (data: components['schemas']['RsDataEmpty']) => void
  ) {
    if (!confirm('삭제하시겠습니까?')) return;

    const { data, error } = await rq
      .apiEndPoints()
      .DELETE('/api/v1/postComments/{postId}/{postCommentId}', {
        params: {
          path: { postId: parseInt($page.params.id), postCommentId: postComment.id }
        }
      });

    if (error) throw error;

    callback(data!);
  }

  async function submitEditCommentForm(id: number) {
    const toastUiEditor = toastUiEditors[id];

    toastUiEditor.editor.setMarkdown(toastUiEditor.editor.getMarkdown().trim());

    if (toastUiEditor.editor.getMarkdown().trim().length === 0) {
      rq.msgError('내용을 입력해주세요.');
      toastUiEditor.editor.focus();
      return;
    }

    const { data, error } = await rq
      .apiEndPoints()
      .PUT('/api/v1/postComments/{postId}/{postCommentId}', {
        params: {
          path: { postId: parseInt($page.params.id), postCommentId: id }
        },
        body: {
          body: toastUiEditor.editor.getMarkdown()
        }
      });

    rq.msgInfo(data!.msg);

    const oldPostComment = postComments.find((e) => e.id === id)!;
    delete toastUiEditors[id];
    Object.assign(oldPostComment, data!.data.item);
  }

  async function makeTempPostComment() {
    const { data } = await rq.apiEndPoints().POST('/api/v1/postComments/{postId}/temp', {
      params: { path: { postId: parseInt($page.params.id) } }
    });

    tempPostCommentId = data!.data.item.id;
    setTimeout(() => {
      toastUiEditor.editor.focus();
    }, 100);
  }

  async function submitWriteCommentForm() {
    writeCommentToastUiEditor.editor.setMarkdown(
      writeCommentToastUiEditor.editor.getMarkdown().trim()
    );

    if (writeCommentToastUiEditor.editor.getMarkdown().trim().length === 0) {
      rq.msgError('내용을 입력해주세요.');
      writeCommentToastUiEditor.editor.focus();
      return;
    }

    const { data, error } = await rq
      .apiEndPoints()
      .PUT('/api/v1/postComments/{postId}/{postCommentId}', {
        params: { path: { postId: parseInt($page.params.id), postCommentId: tempPostCommentId } },
        body: {
          body: writeCommentToastUiEditor.editor.getMarkdown()
        }
      });

    writeCommentToastUiEditor.editor.setMarkdown('');
    tempPostCommentId = 0;

    rq.msgInfo(data!.msg);

    // postComments 맨 앞에 넣고 싶어
    postComments.unshift(data!.data.item);
  }

  function Post__switchTab() {
    var inactiveTabs = document.querySelectorAll('.toastui-editor-tabs > .tab-item:not(.active)');
    inactiveTabs.forEach((value: Element) => {
      (value as HTMLElement).click();
    });
  }

  rq.effect(() => {
    hotkeys.filter = function (event) {
      return true;
    };

    hotkeys('ctrl+p,cmd+p', 'postDetail', function (event, handler) {
      Post__switchTab();
      event.preventDefault();
    });

    hotkeys('ctrl+d,cmd+d', 'postDetail', function (event, handler) {
      toastUiEditor.toggleFullScreen();
      event.preventDefault();
    });

    hotkeys.setScope('postDetail');

    return () => {
      hotkeys.deleteScope('postDetail');
    };
  });
</script>

<div class="flex-grow flex justify-center items-center">
  <div class="w-full px-2 mt-4">
    {#await loadPost()}
      <div>loading...</div>
    {:then { data: { item: post } }}
      <div class="card bg-base-100 shadow">
        <div class="card-body">
          <div class="detail grid grid-cols-[repeat(auto-fit,minmax(100px,1fr))] gap-3">
            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">번호</span>
              </label>
              <div>{post.id}</div>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">작성일</span>
              </label>
              <div>{prettyDateTime(post.createDate)}</div>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">작성자</span>
              </label>
              <div>{post.authorName}</div>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">공개여부</span>
              </label>
              {#if post.published}
                <i class="fa-regular fa-eye"></i>
              {:else}
                <i class="fa-regular fa-eye-slash"></i>
              {/if}
            </div>

            <div class="form-control col-span-full">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">제목</span>
              </label>
              <div>{post.title}</div>
            </div>

            <div class="flex gap-2">
              {#if post.actorCanDelete}
                <button
                  class="btn btn-outline"
                  onclick={() => rq.confirmAndDeletePost(post, '/p/list')}>삭제</button
                >
              {/if}

              {#if post.actorCanEdit}
                <a class="btn btn-outline" href="/p/{post.id}/edit">수정</a>
              {/if}
            </div>

            <div class="form-control col-span-full">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">내용</span>
              </label>

              {#key post.id}
                <ToastUiEditor bind:this={toastUiEditor} body={post.body} viewer={true}>
                  <div slot="beforeContent">
                    <!-- svelte-ignore a11y-click-events-have-key-events -->
                    <!-- svelte-ignore a11y-no-noninteractive-element-interactions -->
                    <h1>
                      제목 : {post.title},
                      <div class="tooltip tooltip-right" data-tip="Ctrl + d, Cmd + d">
                        <button class="btn btn-sm" on:click={() => toastUiEditor.toggleFullScreen()}
                          >전체화면</button
                        >
                      </div>
                    </h1>
                  </div>
                </ToastUiEditor>
              {/key}
            </div>
          </div>
        </div>
      </div>
    {:catch error}
      {error.msg}
    {/await}

    <div>
      <h1 class="font-bold text-2xl">댓글작성</h1>

      <form on:submit|preventDefault={submitWriteCommentForm}>
        {#if tempPostCommentId == 0}
          <input type="text" class="input input-bordered" onclick={() => makeTempPostComment()} />
        {/if}

        {#if tempPostCommentId > 0}
          <div>
            <div>내용</div>
            {#key tempPostCommentId}
              <ToastUiEditor
                bind:this={writeCommentToastUiEditor}
                body={''}
                saveBody={() => submitWriteCommentForm()}
              />
            {/key}
          </div>

          <div>
            <button class="btn btn-outline" type="submit">댓글작성</button>
          </div>
        {/if}
      </form>
    </div>

    {#await loadPostComments()}
      <div>loading...</div>
    {:then { }}
      <h1 class="font-bold text-2xl">댓글</h1>

      <div>
        {#each postComments as postComment}
          <div class="border">
            <div>번호 : {postComment.id}</div>
            <div>작성 : {prettyDateTime(postComment.createDate)}</div>
            <div>수정 : {prettyDateTime(postComment.modifyDate)}</div>
            <div>작성자 : {postComment.authorName}</div>
            <div>
              <img src={postComment.authorProfileImgUrl} width="30" class="rounded-full" alt="" />
            </div>

            {#if !postComment.editing}
              <div>
                {#key postComment.id}
                  <ToastUiEditor body={postComment.body} viewer={true} />
                {/key}
              </div>

              <div>
                {#if postComment.actorCanDelete}
                  <button
                    class="btn btn-outline"
                    onclick={() =>
                      confirmAndDeletePostComment(postComment, (data) => {
                        rq.msgInfo(data.msg);
                        postComments.splice(postComments.indexOf(postComment), 1);
                      })}>삭제</button
                  >
                {/if}

                {#if postComment.actorCanEdit}
                  <button
                    class="btn btn-outline"
                    onclick={() => (postComment.editing = !postComment.editing)}>수정</button
                  >
                {/if}
              </div>
            {/if}

            {#if postComment.editing}
              <div>
                <form on:submit|preventDefault={() => submitEditCommentForm(postComment.id)}>
                  <input type="hidden" name="id" value={postComment.id} />

                  <div>
                    <div>내용</div>
                    {#key postComment.id}
                      <ToastUiEditor
                        bind:this={toastUiEditors[postComment.id]}
                        body={postComment.body}
                        saveBody={() => submitEditCommentForm(postComment.id)}
                      />
                    {/key}
                  </div>

                  <div>
                    <button class="btn btn-outline" type="submit">저장</button>
                    <button
                      type="button"
                      class="btn btn-outline"
                      onclick={() => (postComment.editing = !postComment.editing)}>수정취소</button
                    >
                  </div>
                </form>
              </div>
            {/if}
          </div>
        {/each}
      </div>
    {/await}
  </div>
</div>
