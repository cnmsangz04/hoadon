<template>
  <div class="sidebar">
    <div class="sidebar-inner">

      <!-- Logo -->
      <div class="brand">
        <router-link class="brand-link" to="/">
          <img class="logo" :src="require('@/assets/images/logo/logo-hoadon.png')" alt="logo" />
        </router-link>
      </div>

      <!-- Menu -->
      <nav class="menu">
        <ul>
          <li
            v-for="(item, index) in menu"
            :key="index"
            :class="{
              'has-children': !!item.children,
              open: openIndex === index,
              active: isActive(item),
            }"
          >
            <!-- Item có children -->
            <template v-if="item.children">
              <a href="#" class="menu-item" @click.prevent="toggle(index)">
                <i :class="item.icon"></i>
                <span class="label">{{ item.title }}</span>
                <i class="chev fas fa-chevron-down" :class="{ rotated: openIndex === index }"></i>
              </a>

              <transition name="slide">
                <ul class="sub" v-show="openIndex === index">
                  <li
                    v-for="(c, ci) in item.children"
                    :key="ci"
                    :class="{ active: isActive(c) }"
                  >
                    <router-link :to="c.to">{{ c.title }}</router-link>
                  </li>
                </ul>
              </transition>
            </template>

            <!-- Item không có children -->
            <template v-else>
              <router-link class="menu-item" :to="item.to">
                <i :class="item.icon"></i>
                <span class="label">{{ item.title }}</span>
              </router-link>
            </template>

          </li>
        </ul>
      </nav>

      <div class="sidebar-footer">v1.0</div>
    </div>
  </div>
</template>
<script>
export default {
  name: "sidebar_administrator",

  data() {
    return {
      openIndex: null,
      menu: [
        { title: 'Trang chủ', icon: 'fas fa-home', to: '/' },
      ]
    };
  },

  mounted() {
    // Tự động mở menu chứa route đang active
    this.menu.forEach((item, idx) => {
      if (item.children && this.isActive(item)) {
        this.openIndex = idx;
      }
    });
  },

  methods: {
    toggle(index) {
      this.openIndex = this.openIndex === index ? null : index;
    },

    isActive(item) {
      if (!item) return false;

      if (item.to) {
        return this.$route.path.indexOf(item.to) === 0;
      }
      if (item.children) {
        return item.children.some(c => this.$route.path.indexOf(c.to) === 0);
      }

      return false;
    },
  }
};
</script>
<style scoped>
.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #0f1724 0%, #0b2238 100%);
  color: #e6eef8;
  height: 100vh;
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(2,6,23,0.6);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial;
}

.sidebar-inner {
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.brand { text-align: center; margin-bottom: 12px; }
.logo { max-width: 160px; display: block; margin: auto; }

.menu { overflow: auto; flex: 1; }
.menu ul { list-style: none; padding: 0; margin: 0; }
.menu-item { display: flex; align-items: center; gap: 10px; padding: 10px 12px; color: #dbeefd; border-radius: 6px; text-decoration: none; }
.menu-item:hover { background: rgba(255,255,255,0.03); color: #fff; }
.menu-item i { width: 18px; text-align: center; }

.menu li.active > .menu-item {
  background: linear-gradient(90deg, rgba(255,255,255,0.06), rgba(255,255,255,0.03));
}

.has-children .chev {
  margin-left: auto;
  transition: transform 0.25s;
}

.rotated { transform: rotate(-180deg); }

.sub { padding-left: 10px; margin: 5px 0 10px; }
.sub li a {
  display: block; padding: 8px 12px; border-radius: 6px; color: #cfe6ff;
}
.sub li.active > a { background: rgba(255,255,255,0.03); color: #fff; }

.sidebar-footer {
  text-align: center; padding: 8px 0; color: #7f9ebf; font-size: 12px;
}

/* Animation */
.slide-enter-active, .slide-leave-active { transition: 240ms; }
.slide-enter, .slide-leave-to { opacity: 0; transform: translateY(-6px); height: 0; }
</style>

