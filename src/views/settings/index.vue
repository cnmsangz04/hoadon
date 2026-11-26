<template>
  <div class="container-fluid py-3 settings-home">
    <div class="page-head">
      <div>
        <h4>{{ $t('settingsHome.title') }}</h4>
        <p>{{ $t('settingsHome.description') }}</p>
      </div>
    </div>

    <div class="settings-grid">
      <router-link class="settings-tile" to="/setting/account/list">
        <i class="fas fa-user-cog"></i>
        <span>{{ $t('header.account') }}</span>
        <small>{{ $t('settingsHome.accountHint') }}</small>
      </router-link>
      <router-link class="settings-tile" to="/setting/profile/list">
        <i class="fas fa-id-card"></i>
        <span>{{ $t('settingsHome.profile') }}</span>
        <small>{{ $t('settingsHome.profileHint') }}</small>
      </router-link>
      <router-link class="settings-tile" to="/setting/sessions/list">
        <i class="fas fa-desktop"></i>
        <span>{{ $t('settingsHome.sessions') }}</span>
        <small>{{ $t('settingsHome.sessionsHint') }}</small>
      </router-link>
      <router-link class="settings-tile" to="/setting/security/ip">
        <i class="fas fa-shield-alt"></i>
        <span>{{ $t('settingsHome.ipSecurity') }}</span>
        <small>{{ $t('settingsHome.ipSecurityHint') }}</small>
      </router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SettingsIndex'
}
</script>

<style scoped>
.settings-home {
  font-size: 13px;
}

.page-head {
  margin-bottom: 16px;
}

.page-head h4 {
  color: #1f2937;
  font-weight: 700;
  margin: 0 0 4px;
}

.page-head p {
  color: #64748b;
  margin: 0;
}

.settings-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.settings-tile {
  background: #fff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  color: #1f2937;
  display: grid;
  gap: 6px;
  padding: 16px;
  text-decoration: none;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.settings-tile:hover {
  border-color: #bfdbfe;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  text-decoration: none;
  transform: translateY(-2px);
}

.settings-tile i {
  color: #2563eb;
  font-size: 22px;
}

.settings-tile span {
  font-weight: 700;
}

.settings-tile small {
  color: #64748b;
  line-height: 1.4;
}
</style>
