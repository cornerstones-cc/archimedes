{
  template: `<div style="text-align: center;;margin:auto">
    <a-result v-if="errorMessage" status="warning" :title="errorMessage" />
    <a-spin v-else-if="isJump" tip="Go OKTA..." size="large" />
    <a-spin v-else-if="code" tip="Login..." size="large" />
    <a-button v-else type="primary" @click="jump">
      Jump to the login page of the {{ loginTypeName }}
    </a-button>
  </div>`,
  data () {
    return {
      errorMessage: '',
      code: null,
      redirectUri: window.location.protocol + '//' + window.location.host + window.location.pathname,
      isJump: false
    }
  },
  props: {
    loginType: {
      type: Object,
      default: null
    }
  },
  computed: {
    loginTypeName () {
      return this.loginType.name
    }
  },
  created () {
  if (this.$route.query.error) {
      this.errorMessage = this.$route.query.error_description.replace('+', ' ')
    } else if (this.$route.query.code) {
      this.code = this.$route.query.code
      setTimeout(() => {
        this.$emit('finished', {
          code: this.code,
          redirect_uri: this.redirectUri
        })
      }, 1000)
    }
  },
  methods: {
    jump () {
      this.isJump = true
      window.location.href =
        'https://mars-group.okta.com/oauth2/v1/authorize' +
        '?client_id=' +
        window.serverConfig.VUE_APP_OKTA_CLIENT +
        '&nonce=' +
        new Date().getTime() +
        '&state=' +
        new Date().getTime() +
        '&response_type=code&scope=openid profile' +
        '&redirect_uri=' +
        this.redirectUri
    }
  }
}