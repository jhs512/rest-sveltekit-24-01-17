<script lang="ts">
  import { page } from '$app/stores';

  import rq from '$lib/rq/rq.svelte';
  import { prettyDateTime } from '$lib/utils';
  import type { components } from '$lib/types/api/v1/schema';
  import Pagination from '$lib/components/Pagination.svelte';
  import type { KwTypeV1 } from '$lib/types';

  let posts: components['schemas']['PostDto'][] = $state([]);

  async function load() {
    if (import.meta.env.SSR) throw new Error('CSR ONLY');

    const kw = $page.url.searchParams.get('kw') ?? '';
    const kwType = ($page.url.searchParams.get('kwType') ?? 'ALL') as KwTypeV1;
    const page_ = parseInt($page.url.searchParams.get('page') ?? '1');

    const { data } = await rq.apiEndPoints().GET('/api/v1/posts', {
      params: {
        query: {
          kw,
          kwType,
          page: page_
        }
      }
    });

    posts = data!.data.itemPage.content;

    return data!;
  }
</script>

<div class="flex-grow flex justify-center items-center">
  <div class="w-full max-w-screen-2xl">
    {#await load()}
      <p>loading...</p>
    {:then { data: { itemPage } }}
      <div class="flex gap-2 items-center mt-4 px-2">
        <div class="badge badge-outline">
          검색결과 : {itemPage.totalElementsCount}건
        </div>

        <div class="flex-grow"></div>

        <button
          class="btn btn-primary"
          onclick={() => {
        const modal = document.querySelector('#search_modal_1') as HTMLDialogElement;
        modal.showModal();

        const inputKw = modal.querySelector('input[name="kw"]') as HTMLInputElement;
        inputKw.focus();
      }}
        >
          <i class="fa-solid fa-magnifying-glass"></i> 검색
          {#if $page.url.searchParams.get('kw')}
            <span class="text-sm text-gray-400">(검색어 : {$page.url.searchParams.get('kw')})</span>
          {/if}
        </button>

        {#if $page.url.searchParams.get('kw')}
          <a class="btn" href="/p/list">
            <i class="fa-solid fa-xmark"></i> 검색어 지우기
          </a>
        {/if}
      </div>

      <dialog id="search_modal_1" class="modal">
        <div class="modal-box">
          <h3 class="font-bold text-lg">검색</h3>
          <form
            action="/p/list"
            class="bg-base rounded grid grid-cols-1 gap-4"
            onsubmit={() => {
          const modal = document.querySelector('#search_modal_1') as HTMLDialogElement;
          modal.close();
        }}
          >
            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">검색필터</span>
              </label>

              <select
                name="kwType"
                class="select select-bordered"
                value={$page.url.searchParams.get('kwType') ?? 'ALL'}
              >
                <option value="ALL">전체</option>
                <option value="TITLE">제목</option>
                <option value="BODY">내용</option>
                <option value="NAME">작성자</option>
              </select>
            </div>

            <div class="form-control">
              <!-- svelte-ignore a11y-label-has-associated-control -->
              <label class="label">
                <span class="label-text">검색어</span>
              </label>

              <input
                placeholder="검색어"
                class="input input-bordered"
                name="kw"
                type="search"
                value={$page.url.searchParams.get('kw') ?? ''}
                autocomplete="off"
              />
            </div>

            <div>
              <button class="btn btn-block btn-primary gap-1">
                <i class="fa-solid fa-magnifying-glass"></i>
                <span>검색</span>
              </button>
            </div>
          </form>
        </div>
        <form method="dialog" class="modal-backdrop">
          <button>close</button>
        </form>
      </dialog>

      <div class="mt-4">
        <Pagination page={itemPage} />
      </div>

      <ul class="mt-4 grid grid-cols-1 gap-3 px-2">
        {#each posts as post}
          <li>
            <div class="flex items-center gap-2">
              <a href="/p/{post.id}">{post.id}. {post.title}</a>
              <span>추천 : {post.likesCount}</span>
              <span>작성일 : {prettyDateTime(post.createDate)}</span>

              {#if post.actorCanDelete}
                <button
                  onclick={() =>
                    rq.confirmAndDeletePost(post, () => {
                      posts.splice(posts.indexOf(post), 1);
                    })}>삭제</button
                >
              {/if}

              {#if post.actorCanEdit}
                <a href="/p/{post.id}/edit">수정</a>
              {/if}

              {#if post.actorCanLike}
                <button
                  onclick={() =>
                    rq.like(post, (data) => {
                      Object.assign(post, data.data.item);
                      rq.msgInfo(data.msg);
                    })}>추천하기</button
                >
              {/if}

              {#if post.actorCanCancelLike}
                <button
                  onclick={() =>
                    rq.cancelLike(post, (data) => {
                      Object.assign(post, data.data.item);
                      rq.msgInfo(data.msg);
                    })}>추천취소</button
                >
              {/if}

              <span class="flex-1"></span>

              <span>작성자 : {post.authorName}</span>
              <img src={post.authorProfileImgUrl} width="50" class="rounded-full" alt="" />
            </div>
          </li>
        {/each}
      </ul>

      <div class="mt-4 mb-4">
        <Pagination page={itemPage} />
      </div>
    {/await}
  </div>
</div>
