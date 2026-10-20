<template>
  <main class="celebration" :class="[`scene-${step}`, { ready: pageReady, 'envelope-open': envelopeOpen }]">
    <div class="backdrop" aria-hidden="true">
      <span class="aurora aurora-pink"></span>
      <span class="aurora aurora-violet"></span>
      <span class="moon-ring"></span>
      <i v-for="spark in sparks" :key="`spark-${spark.id}`" class="spark" :style="spark.style"></i>
      <i v-for="petal in petals" :key="`petal-${petal.id}`" class="petal" :style="petal.style"></i>
    </div>

    <div class="effects" aria-hidden="true">
      <span v-for="heart in hearts" :key="heart.id" class="heart" :style="heart.style">♥</span>
      <i v-for="piece in confetti" :key="piece.id" class="confetti" :style="piece.style"></i>
    </div>

    <header class="topbar">
      <div class="brand"><span>✦</span><b>Một món quà nhỏ</b></div>
      <div class="topbar-right">
        <span class="scene-count">{{ sceneNumber }} <i>/ 03</i></span>
        <button class="music-button" type="button" :aria-label="musicOn ? 'Tắt nhạc' : 'Bật nhạc'" @click="toggleMusic">
          <span v-if="musicOn" class="sound-wave"><i></i><i></i><i></i></span>
          <span v-else>♪</span>
        </button>
      </div>
    </header>

    <section class="stage" aria-live="polite">
      <transition name="scene" mode="out-in">
        <div v-if="step === 'intro'" key="intro" class="scene intro-scene">
          <div class="intro-layout">
            <div class="intro-copy">
              <div class="date-monogram" aria-hidden="true"><b>20</b><i></i><b>10</b></div>
              <span class="eyebrow"><i></i>Một món quà nhỏ<i></i></span>
              <h1>Điều <em>dịu dàng</em><br>này dành riêng cho {{ recipientLabel }}.</h1>
              <p>Gửi riêng <strong>{{ recipientLabel }}</strong> trong ngày Phụ nữ Việt Nam.</p>
            </div>

            <div class="envelope-stage" aria-label="Phong bì đang tự động mở">
              <span class="envelope-glow"></span>
              <span class="orbit orbit-one"></span>
              <span class="orbit orbit-two"></span>
              <span class="envelope-spark spark-one">✦</span>
              <span class="envelope-spark spark-two">✧</span>
              <div class="envelope">
                <span class="envelope-back"></span>
                <span class="letter-preview">
                  <small>Gửi tặng</small>
                  <b>{{ recipientLabel }}</b>
                  <i>♡</i>
                </span>
                <span class="envelope-front"></span>
                <span class="envelope-flap"></span>
                <span class="wax-seal">N</span>
              </div>
            </div>
          </div>

          <div class="auto-note intro-progress" style="--progress-duration: 5s"><span></span><small>Đang mở món quà cho {{ recipientLabel }}</small></div>
        </div>

        <div v-else-if="step === 'flowers'" key="flowers" class="scene flowers-scene">
          <div class="flower-layout">
            <div class="bouquet-stage">
              <span class="bouquet-aura"></span>
              <span class="bouquet-ring ring-one"></span>
              <span class="bouquet-ring ring-two"></span>
              <span class="bouquet-shine shine-one">✦</span>
              <span class="bouquet-shine shine-two">✧</span>
              <span class="bouquet-shine shine-three">✦</span>
              <img :src="bouquetImage" alt="Bó hoa hồng và tulip dành tặng ngày 20 tháng 10">
            </div>

            <div class="flower-copy">
              <span class="eyebrow"><i></i>20 · 10 · 2026<i></i></span>
              <p class="flower-kicker">Happy Vietnamese Women's Day</p>
              <h2>Một bó hoa<br><span>dành riêng cho</span><br><em>{{ recipientLabel }}</em></h2>
              <div class="flower-message">
                <span>“</span>
              <p>Mong mọi điều đẹp đẽ sẽ luôn tìm thấy {{ wishTarget }},<br>theo cách dịu dàng nhất.</p>
              </div>
            </div>
          </div>
          <div class="auto-note flower-progress" style="--progress-duration: 8s"><span></span><small>Lời chúc đang đến</small></div>
        </div>

        <div v-else key="letter" class="scene letter-scene">
          <article class="letter-card">
            <span class="paper-light"></span>
            <span class="card-flower-mark">❀</span>
            <header class="letter-head">
              <span>20 · 10 · 2026</span>
              <span>Vietnamese Women's Day</span>
            </header>

            <div class="letter-content">
              <p class="recipient">Gửi {{ recipientLabel }},</p>
              <h2>{{ letterTitleLead }}<br><em>rạng rỡ.</em></h2>

              <div class="wish-list">
                <p v-for="(line, index) in visibleMessages" :key="index">{{ line }}</p>
                <i v-if="visibleMessages.length < messageLines.length" class="typing-dot"></i>
              </div>

              <transition name="sign">
                <div v-if="letterComplete" class="signature">Với tất cả sự trân trọng <span>♡</span></div>
              </transition>
            </div>

            <img class="card-bouquet" :src="bouquetImage" alt="">
            <span class="card-line"></span>
          </article>

          <transition name="bonus">
            <div v-if="bonusVisible" class="bonus-wish"><span>✦</span>{{ bonusWish }}<span>✦</span></div>
          </transition>

          <button class="replay-button" type="button" @click="restart"><span>↻</span> Xem lại món quà</button>
        </div>
      </transition>
    </section>

    <footer class="footer">
      <span>Made with <b>♡</b> for 20 · 10</span>
      <div class="steps" aria-hidden="true"><i :class="{ active: step === 'intro' }"></i><i :class="{ active: step === 'flowers' }"></i><i :class="{ active: step === 'letter' }"></i></div>
    </footer>
  </main>
</template>

<script>
import bouquetImage from '@/assets/images/womens-day-bouquet.webp'
import musicFile from '@/assets/audio/womens-day-original.wav'

export default {
  name: 'WomensDay',
  data () {
    const rawName = new URLSearchParams(window.location.search).get('to') || ''
    return {
      bouquetImage,
      musicSource: musicFile,
      recipient: String(rawName).replace(/[<>"']/g, '').trim().slice(0, 32),
      relationshipKey: String(new URLSearchParams(window.location.search).get('c') || '').toLowerCase(),
      step: 'intro',
      pageReady: false,
      envelopeOpen: false,
      visibleMessages: [],
      letterComplete: false,
      bonusVisible: false,
      bonusWish: '',
      hearts: [],
      confetti: [],
      timers: [],
      heartInterval: null,
      musicOn: false,
      musicAudio: null,
      musicStartTimer: null,
      musicFadeTimer: null,
      musicUnlockHandler: null,
      sparks: Array.from({ length: 34 }, (_, id) => ({
        id,
        style: {
          left: `${Math.random() * 100}%`,
          top: `${Math.random() * 100}%`,
          '--size': `${1 + Math.random() * 2.6}px`,
          '--delay': `${Math.random() * 6}s`,
          '--duration': `${3.5 + Math.random() * 5}s`
        }
      })),
      petals: Array.from({ length: 15 }, (_, id) => ({
        id,
        style: {
          left: `${Math.random() * 100}%`,
          '--delay': `${Math.random() * 10}s`,
          '--duration': `${11 + Math.random() * 8}s`,
          '--drift': `${-60 + Math.random() * 120}px`,
          '--scale': 0.55 + Math.random() * 0.7
        }
      })),
    }
  },
  computed: {
    displayName () {
      return this.recipient || 'người đặc biệt'
    },
    addressWord () {
      return { c: 'chị', b: 'bạn', e: 'em', m: 'má mi' }[this.relationshipKey] || ''
    },
    addressCapitalized () {
      const word = this.addressWord || 'người đặc biệt'
      return word.charAt(0).toUpperCase() + word.slice(1)
    },
    recipientLabel () {
      if (this.recipient) return this.addressWord ? `${this.addressWord} ${this.recipient}` : this.recipient
      return this.addressWord || 'người đặc biệt'
    },
    wishTarget () {
      return this.addressWord || 'người đặc biệt'
    },
    letterTitleLead () {
      return this.addressWord ? `Chúc ${this.addressWord} luôn` : 'Chúc ngày 20/10 thật'
    },
    messageLines () {
      return [
        this.addressWord
          ? `Chúc ${this.addressWord} luôn xinh đẹp, rạng rỡ và hạnh phúc.`
          : 'Chúc ngày 20/10 thật xinh đẹp, rạng rỡ và hạnh phúc.',
        'Mong mỗi ngày trôi qua đều mang đến thật nhiều niềm vui, những điều ngọt ngào và những khoảnh khắc đáng nhớ.',
        this.addressWord
          ? `Không chỉ riêng ngày 20/10, hy vọng ${this.addressWord} sẽ luôn được yêu thương, trân trọng và mỉm cười mỗi ngày. ❤️`
          : 'Không chỉ riêng ngày 20/10, hy vọng mỗi ngày đều được yêu thương, trân trọng và ngập tràn nụ cười. ❤️'
      ]
    },
    sceneNumber () {
      return this.step === 'intro' ? '01' : this.step === 'flowers' ? '02' : '03'
    }
  },
  mounted () {
    document.title = `Một món quà nhỏ dành cho ${this.displayName}`
    this.bonusWish = this.pickBonusWish()
    this.startSequence()
    this.musicUnlockHandler = (event) => {
      if (event.target && event.target.closest && event.target.closest('.music-button')) return
      if (!this.musicOn) this.startMusic()
    }
    window.addEventListener('pointerdown', this.musicUnlockHandler, { passive: true })
    window.addEventListener('keydown', this.musicUnlockHandler, { passive: true })
  },
  beforeDestroy () {
    this.clearSequence()
    if (this.musicUnlockHandler) {
      window.removeEventListener('pointerdown', this.musicUnlockHandler)
      window.removeEventListener('keydown', this.musicUnlockHandler)
    }
    if (this.musicAudio) {
      window.clearTimeout(this.musicStartTimer)
      window.clearTimeout(this.musicFadeTimer)
      this.musicAudio.pause()
      this.musicAudio.currentTime = 0
    }
  },
  methods: {
    later (callback, delay) {
      const timer = window.setTimeout(callback, delay)
      this.timers.push(timer)
      return timer
    },
    clearSequence () {
      this.timers.forEach(timer => window.clearTimeout(timer))
      this.timers = []
      if (this.heartInterval) window.clearInterval(this.heartInterval)
      this.heartInterval = null
    },
    startSequence () {
      this.clearSequence()
      this.step = 'intro'
      this.pageReady = false
      this.envelopeOpen = false
      this.visibleMessages = []
      this.letterComplete = false
      this.bonusVisible = false
      this.hearts = []
      this.confetti = []

      this.later(() => { this.pageReady = true }, 160)
      this.later(() => { this.envelopeOpen = true }, 1050)
      this.later(() => {
        this.step = 'flowers'
        this.burstConfetti(42)
      }, 5000)
      this.later(() => { this.openLetter() }, 13000)
    },
    openLetter () {
      this.step = 'letter'
      this.visibleMessages = []
      this.messageLines.forEach((line, index) => {
        const delays = [800, 3200, 5600]
        this.later(() => { this.visibleMessages.push(line) }, delays[index])
      })
      this.later(() => {
        this.letterComplete = true
        this.sendHearts()
      }, 6900)
      this.later(() => {
        this.bonusVisible = true
        this.burstConfetti(34)
      }, 8500)
      this.heartInterval = window.setInterval(() => {
        if (this.step === 'letter') this.sendHearts()
      }, 7000)
    },
    restart () {
      this.bonusWish = this.pickBonusWish()
      window.scrollTo({ top: 0, behavior: 'smooth' })
      this.startSequence()
    },
    pickBonusWish () {
      const wishes = [
        `${this.addressCapitalized} xứng đáng với tất cả những điều dịu dàng nhất.`,
        `Mong nụ cười của ${this.wishTarget} luôn rực rỡ hơn những vì sao.`,
        `Chúc mỗi ngày của ${this.wishTarget} đều có hoa, có nắng và yêu thương.`
      ]
      return wishes[Math.floor(Math.random() * wishes.length)]
    },
    sendHearts () {
      const stamp = Date.now()
      this.hearts = Array.from({ length: 22 }, (_, id) => ({
        id: `${stamp}-${id}`,
        style: {
          left: `${8 + Math.random() * 84}%`,
          bottom: `${4 + Math.random() * 18}%`,
          '--x': `${-100 + Math.random() * 200}px`,
          '--delay': `${Math.random() * 0.7}s`,
          '--duration': `${2.7 + Math.random() * 1.8}s`,
          '--size': `${12 + Math.random() * 21}px`
        }
      }))
      this.later(() => { this.hearts = [] }, 5100)
    },
    burstConfetti (amount) {
      const colors = ['#f4a6bd', '#f8d997', '#d7b7ef', '#fff0cf', '#cf6f91']
      const stamp = Date.now()
      this.confetti = Array.from({ length: amount }, (_, id) => ({
        id: `${stamp}-${id}`,
        style: {
          left: `${43 + Math.random() * 14}%`,
          top: `${30 + Math.random() * 14}%`,
          '--x': `${-230 + Math.random() * 460}px`,
          '--y': `${100 + Math.random() * 310}px`,
          '--rotate': `${Math.random() * 720}deg`,
          '--delay': `${Math.random() * 0.3}s`,
          '--duration': `${1.8 + Math.random() * 1.5}s`,
          backgroundColor: colors[id % colors.length]
        }
      }))
      this.later(() => { this.confetti = [] }, 3600)
    },
    startMusic () {
      if (!this.musicAudio) {
        this.musicAudio = new Audio(this.musicSource)
        this.musicAudio.loop = true
        this.musicAudio.preload = 'auto'
      }
      this.musicOn = true
      window.clearTimeout(this.musicStartTimer)
      window.clearTimeout(this.musicFadeTimer)
      this.musicAudio.volume = 0.0001
      this.musicStartTimer = window.setTimeout(() => {
        this.musicAudio.play().then(() => {
          const startedAt = performance.now()
          const fadeIn = () => {
            if (!this.musicOn || !this.musicAudio || this.musicAudio.paused) return
            const progress = Math.min(1, (performance.now() - startedAt) / 2200)
            this.musicAudio.volume = 0.0001 + (0.42 - 0.0001) * progress
            if (progress < 1) this.musicFadeTimer = window.setTimeout(fadeIn, 80)
          }
          fadeIn()
        }).catch(() => {})
      }, 900)
    },
    toggleMusic () {
      if (this.musicOn) {
        this.musicOn = false
        window.clearTimeout(this.musicStartTimer)
        window.clearTimeout(this.musicFadeTimer)
        if (this.musicAudio) this.musicAudio.pause()
        return
      }
      this.startMusic()
    }
  }
}
</script>

<style scoped>
:global(html), :global(body), :global(#app) { min-height: 100%; margin: 0; }
:global(body) { background: #160914; }

.celebration {
  --cream: #fff9f0;
  --gold: #f2d28f;
  --rose: #ee91ad;
  position: relative;
  min-height: 100vh;
  min-height: 100svh;
  overflow: hidden;
  color: var(--cream);
  background: radial-gradient(circle at 50% 38%, #5a2549 0, #2c122b 34%, #160a1b 72%, #100711 100%);
  font-family: "Segoe UI", Arial, sans-serif;
  isolation: isolate;
}
.celebration:before { position: absolute; inset: 14px; z-index: 7; border: 1px solid rgba(255,238,222,.14); border-radius: 28px; content: ''; pointer-events: none; }
.celebration:after { position: fixed; inset: 0; z-index: 1; background: radial-gradient(circle, transparent 46%, rgba(7,0,9,.5) 100%); content: ''; pointer-events: none; }
.backdrop, .effects { position: fixed; inset: 0; overflow: hidden; pointer-events: none; }
.backdrop { z-index: 0; }
.effects { z-index: 20; }
.aurora { position: absolute; border-radius: 50%; filter: blur(85px); opacity: .28; animation: drift 12s ease-in-out infinite; }
.aurora-pink { width: 440px; height: 440px; top: -230px; right: -80px; background: #e65b97; }
.aurora-violet { width: 480px; height: 480px; bottom: -280px; left: -170px; background: #7549bd; animation-direction: reverse; animation-duration: 15s; }
.moon-ring { position: absolute; left: 50%; top: 49%; width: min(52vw, 650px); aspect-ratio: 1; border: 1px solid rgba(245,205,145,.07); border-radius: 50%; box-shadow: 0 0 0 70px rgba(238,132,171,.018), 0 0 100px rgba(238,132,171,.08); transform: translate(-50%,-50%); }
.spark { position: absolute; width: var(--size); height: var(--size); border-radius: 50%; background: #fff3c8; box-shadow: 0 0 9px #ffe6a5; opacity: .25; animation: twinkle var(--duration) ease-in-out var(--delay) infinite; }
.petal { position: absolute; top: -25px; width: 10px; height: 17px; border-radius: 90% 0 90% 0; background: linear-gradient(145deg, rgba(255,193,211,.72), rgba(202,88,133,.25)); animation: petal-fall var(--duration) linear var(--delay) infinite; }
.topbar, .footer { position: relative; z-index: 10; display: flex; align-items: center; justify-content: space-between; width: min(1180px, calc(100% - 92px)); margin: 0 auto; }
.topbar { height: 82px; }
.brand { display: flex; align-items: center; gap: 11px; color: rgba(255,248,239,.67); font-size: 9px; letter-spacing: .18em; text-transform: uppercase; }
.brand > span { display: grid; place-items: center; width: 29px; height: 29px; border: 1px solid rgba(242,210,143,.38); border-radius: 50%; color: var(--gold); box-shadow: 0 0 22px rgba(242,210,143,.12); }
.brand b { font-weight: 700; }
.topbar-right { display: flex; align-items: center; gap: 18px; }
.scene-count { color: rgba(255,248,239,.76); font-family: Georgia, serif; font-size: 12px; letter-spacing: .13em; }.scene-count i { color: rgba(255,255,255,.28); font-style: normal; }
.music-button { display: grid; place-items: center; width: 38px; height: 38px; border: 1px solid rgba(255,231,210,.22); border-radius: 50%; color: var(--gold); background: rgba(255,255,255,.04); cursor: pointer; transition: .25s ease; }.music-button:hover { border-color: rgba(242,210,143,.65); background: rgba(255,255,255,.09); transform: translateY(-2px); }
.sound-wave { display: flex; align-items: center; gap: 2px; height: 13px; }.sound-wave i { width: 2px; height: 7px; border-radius: 3px; background: var(--gold); animation: equalize .7s ease-in-out infinite alternate; }.sound-wave i:nth-child(2) { height: 13px; animation-delay: -.2s; }.sound-wave i:nth-child(3) { height: 9px; animation-delay: -.4s; }
.stage { position: relative; z-index: 5; display: grid; place-items: center; width: min(1120px, calc(100% - 60px)); min-height: calc(100svh - 142px); margin: 0 auto; }
.scene { width: 100%; text-align: center; }
.eyebrow { display: flex; align-items: center; justify-content: center; gap: 11px; color: var(--gold); font-size: 8px; font-weight: 700; letter-spacing: .24em; text-transform: uppercase; }.eyebrow i { display: inline-block; width: 30px; height: 1px; background: linear-gradient(90deg, transparent, var(--gold)); }.eyebrow i:last-child { transform: rotate(180deg); }

.intro-scene { display: grid; grid-template-columns: .9fr 1.1fr; grid-template-areas: "copy envelope" "note envelope"; align-items: center; max-width: 980px; opacity: 0; transform: translateY(12px); transition: .8s ease; }.ready .intro-scene { opacity: 1; transform: none; }
.intro-copy { grid-area: copy; text-align: left; }.intro-copy .eyebrow { justify-content: flex-start; }.intro-copy h1 { margin: 19px 0 20px !important; color: var(--cream) !important; font-family: Georgia, "Times New Roman", serif !important; font-size: clamp(50px, 5.2vw, 75px) !important; font-weight: 400 !important; letter-spacing: -.055em !important; line-height: .98 !important; }.intro-copy h1 em { color: #ffc5d5; font-weight: 400; }.intro-copy p { margin: 0; color: rgba(255,244,235,.55); font-family: Georgia, serif; font-size: 13px; line-height: 1.7; }.intro-copy strong { color: #f7d4dc; font-weight: 400; font-style: italic; }
.envelope-stage { grid-area: envelope; position: relative; display: grid; place-items: center; height: 490px; perspective: 1000px; }
.envelope-glow { position: absolute; width: 365px; height: 365px; border-radius: 50%; background: radial-gradient(circle, rgba(244,135,171,.22), rgba(218,108,157,.05) 45%, transparent 70%); box-shadow: 0 0 100px rgba(221,94,145,.13); animation: glow-pulse 3.6s ease-in-out infinite; }
.orbit { position: absolute; width: 355px; height: 355px; border: 1px solid rgba(242,210,143,.12); border-radius: 50%; }.orbit:after { position: absolute; top: 28px; left: 45px; width: 5px; height: 5px; border-radius: 50%; background: var(--gold); box-shadow: 0 0 13px var(--gold); content: ''; }.orbit-one { animation: rotate 18s linear infinite; }.orbit-two { width: 285px; height: 285px; border-style: dashed; border-color: rgba(255,192,213,.11); animation: rotate 25s linear infinite reverse; }
.envelope { position: relative; z-index: 3; width: 315px; height: 202px; filter: drop-shadow(0 30px 35px rgba(0,0,0,.35)); transform: translateY(13px) rotate(-3deg); transform-style: preserve-3d; animation: envelope-float 4s ease-in-out infinite; }.envelope > span { position: absolute; display: block; }
.envelope-back { inset: 0; border-radius: 7px; background: linear-gradient(145deg,#f4b2c3,#b64672); }
.letter-preview { left: 33px; top: 18px; z-index: 2; width: 249px; height: 168px; padding-top: 29px; color: #a34869; background: linear-gradient(145deg,#fffaf0,#f7e5df); box-shadow: 0 8px 18px rgba(66,15,42,.16); font-family: Georgia,serif; box-sizing: border-box; transition: transform 1s cubic-bezier(.2,.75,.3,1) .35s; }.letter-preview small { display: block; color: #b98a96; font-size: 10px; letter-spacing: .16em; text-transform: uppercase; }.letter-preview b { display: block; max-width: 210px; margin: 8px auto 0; overflow: hidden; font-size: 24px; font-style: italic; font-weight: 400; text-overflow: ellipsis; white-space: nowrap; }.letter-preview i { display: block; margin-top: 8px; color: #ce6d8c; font-size: 21px; }
.envelope-front { inset: 0; z-index: 3; clip-path: polygon(0 0,50% 60%,100% 0,100% 100%,0 100%); border-radius: 7px; background: linear-gradient(145deg,#ec96af,#b84370); }
.envelope-flap { inset: 0; z-index: 4; clip-path: polygon(0 0,100% 0,50% 66%); border-radius: 7px; background: linear-gradient(160deg,#ffc5d4,#d5688e); transform-origin: top; transition: transform .9s cubic-bezier(.4,0,.2,1), z-index 0s .45s; }
.wax-seal { z-index: 5; left: calc(50% - 23px); bottom: 50px; display: grid !important; place-items: center; width: 46px; height: 46px; border: 1px solid rgba(255,255,255,.22); border-radius: 50%; color: #ffe9e1; background: linear-gradient(145deg,#c6547b,#87304f); box-shadow: 0 7px 15px rgba(72,17,42,.36), inset 0 0 0 4px rgba(255,255,255,.05); font: italic 19px Georgia,serif; transition: opacity .3s ease .1s, transform .45s ease; }
.envelope-open .envelope-flap { z-index: 1; transform: rotateX(180deg); }.envelope-open .letter-preview { transform: translateY(-105px); }.envelope-open .wax-seal { opacity: 0; transform: scale(.5); }
.envelope-spark { position: absolute; z-index: 6; color: var(--gold); text-shadow: 0 0 18px rgba(255,220,145,.7); animation: sparkle 2.2s ease-in-out infinite; }.spark-one { top: 88px; right: 72px; font-size: 25px; }.spark-two { bottom: 92px; left: 72px; color: #ffc7d8; font-size: 17px; animation-delay: -.8s; }
.auto-note { grid-area: note; align-self: start; width: 210px; margin-top: -75px; text-align: left; }.auto-note > span { position: relative; display: block; height: 1px; overflow: hidden; background: rgba(255,255,255,.12); }.auto-note > span:after { position: absolute; inset: 0; background: linear-gradient(90deg,var(--gold),#f59abb); content: ''; transform-origin: left; animation: auto-progress var(--progress-duration, 3.75s) linear 0s both; }.auto-note small { display: block; margin-top: 11px; color: rgba(255,255,255,.4); font-size: 8px; font-weight: 700; letter-spacing: .17em; text-transform: uppercase; }

.flowers-scene { display: flex; flex-direction: column; align-items: center; padding: 4px 0 16px; }.flower-heading { position: relative; z-index: 5; }.flower-heading h2 { margin: 10px 0 -36px !important; color: var(--cream) !important; font-family: Georgia,serif !important; font-size: clamp(49px,5vw,70px) !important; font-weight: 400 !important; letter-spacing: -.055em !important; line-height: 1 !important; }.flower-heading h2 em { color: #ffc4d4; font-weight: 400; }
.bouquet-stage { position: relative; display: grid; place-items: center; width: 520px; height: 445px; }.bouquet-stage img { position: relative; z-index: 4; width: 420px; height: auto; filter: drop-shadow(0 35px 35px rgba(0,0,0,.28)); animation: bouquet-in 1.25s cubic-bezier(.18,.8,.28,1) both, bouquet-float 4.6s ease-in-out 1.25s infinite; }.bouquet-aura { position: absolute; width: 330px; height: 330px; border-radius: 50%; background: radial-gradient(circle,rgba(240,135,171,.25),rgba(116,56,124,.08) 52%,transparent 72%); box-shadow: 0 0 95px rgba(232,106,154,.14); }.bouquet-ring { position: absolute; width: 350px; height: 350px; border: 1px solid rgba(244,214,153,.11); border-radius: 50%; }.ring-one { animation: rotate 24s linear infinite; }.ring-two { width: 285px; height: 285px; border-style: dashed; border-color: rgba(255,205,219,.11); animation: rotate 18s linear infinite reverse; }
.bouquet-shine { position: absolute; z-index: 6; color: var(--gold); text-shadow: 0 0 18px rgba(255,217,145,.8); animation: sparkle 2.2s ease-in-out infinite; }.shine-one { left: 40px; top: 118px; font-size: 26px; }.shine-two { right: 48px; top: 77px; color: #ffd1df; font-size: 19px; animation-delay: -.7s; }.shine-three { right: 53px; bottom: 88px; font-size: 15px; animation-delay: -1.2s; }
.flower-note { position: relative; z-index: 5; margin: -17px 0 12px; color: rgba(255,244,235,.67); font-family: Georgia,serif; font-size: 13px; line-height: 1.7; }.flower-progress { width: 170px; margin: 0; text-align: center; }.flower-progress > span:after { animation-duration: var(--progress-duration, 15s); animation-delay: 0s; }.flower-progress small { margin-top: 9px; }

/* Cinematic layouts: every text block owns its space, with no negative spacing. */
.intro-scene { display: flex; flex-direction: column; align-items: center; max-width: 1040px; opacity: 0; transform: translateY(12px); transition: .8s ease; }
.ready .intro-scene { opacity: 1; transform: none; }
.intro-layout { display: grid; grid-template-columns: .92fr 1.08fr; align-items: center; gap: 72px; width: 100%; min-height: 515px; }
.intro-copy { position: relative; grid-area: auto; text-align: left; }
.intro-copy .eyebrow { justify-content: flex-start; }
.date-monogram { display: flex; align-items: center; gap: 18px; margin-bottom: 28px; color: rgba(255,246,232,.9); font-family: Georgia,serif; line-height: .72; }
.date-monogram b { font-size: clamp(78px,8vw,118px); font-weight: 400; letter-spacing: -.08em; text-shadow: 0 0 50px rgba(255,185,205,.16); }
.date-monogram b:last-child { color: #f0a1b9; font-style: italic; }
.date-monogram i { width: 1px; height: 72px; background: linear-gradient(transparent,var(--gold),transparent); transform: rotate(18deg); }
.intro-copy h1 { margin: 18px 0 18px !important; font-size: clamp(48px,4.7vw,68px) !important; }
.intro-copy p { max-width: 390px; font-size: 14px; }
.envelope-stage { grid-area: auto; height: 460px; }
.intro-progress { align-self: center; width: min(310px,70%); margin: 2px 0 0; text-align: center; }
.intro-progress small { color: rgba(255,255,255,.5); }

.flowers-scene { display: flex; flex-direction: column; align-items: center; padding: 0 0 18px; }
.flower-layout { display: grid; grid-template-columns: 1.1fr .9fr; align-items: center; gap: 58px; width: min(1040px,100%); min-height: 515px; }
.bouquet-stage { width: 100%; height: 515px; }
.bouquet-stage img { width: min(485px,46vw); }
.flower-copy { position: relative; z-index: 7; text-align: left; }
.flower-copy .eyebrow { justify-content: flex-start; }
.flower-kicker { margin: 22px 0 10px; color: var(--gold); font-size: 9px; font-weight: 700; letter-spacing: .22em; text-transform: uppercase; }
.flower-copy h2 { margin: 0 !important; color: var(--cream) !important; font-family: Georgia,serif !important; font-size: clamp(50px,5vw,72px) !important; font-weight: 400 !important; letter-spacing: -.055em !important; line-height: .88 !important; }
.flower-copy h2 span { color: rgba(255,248,239,.65); font-size: .62em; letter-spacing: -.03em; }
.flower-copy h2 em { color: #ffc1d2; font-weight: 400; text-shadow: 0 0 36px rgba(255,143,178,.18); }
.flower-message { display: flex; align-items: flex-start; gap: 12px; width: min(350px,100%); margin-top: 28px; border-left: 1px solid rgba(242,210,143,.55); padding: 13px 17px; color: rgba(255,248,239,.76); background: linear-gradient(90deg,rgba(255,255,255,.07),transparent); box-sizing: border-box; }
.flower-message > span { color: var(--gold); font: 31px Georgia,serif; line-height: .7; }
.flower-message p { margin: 0; font-family: Georgia,serif; font-size: 13px; line-height: 1.65; }
.flower-progress { align-self: center; width: 210px; margin: 0; text-align: center; }

.letter-scene { display: flex; flex-direction: column; align-items: center; padding: 7px 0 20px; }.letter-card { position: relative; width: min(710px,calc(100% - 40px)); min-height: 500px; overflow: hidden; border: 1px solid rgba(255,237,219,.78); border-radius: 4px 34px 4px 34px; color: #4c293b; background: linear-gradient(145deg,rgba(255,252,244,.99),rgba(249,225,225,.98)); box-shadow: 0 38px 110px rgba(3,0,7,.46),0 0 90px rgba(230,102,151,.13); text-align: left; }.letter-card:before { position: absolute; inset: 11px; z-index: 4; border: 1px solid rgba(154,83,107,.16); border-radius: 2px 25px 2px 25px; content: ''; pointer-events: none; }.paper-light { position: absolute; top: -150px; right: -80px; width: 390px; height: 390px; border-radius: 50%; background: radial-gradient(circle,rgba(255,255,255,.75),rgba(239,149,177,.18) 52%,transparent 70%); }.letter-head { position: relative; z-index: 3; display: flex; justify-content: space-between; padding: 31px 42px 18px; color: #a27382; font-size: 8px; font-weight: 800; letter-spacing: .19em; text-transform: uppercase; }.letter-content { position: relative; z-index: 3; width: 64%; padding: 7px 0 42px 55px; box-sizing: border-box; }.recipient { margin: 0; color: #ba5575; font-family: Georgia,serif; font-size: 16px; font-style: italic; }.letter-content h2 { margin: 12px 0 23px !important; color: #4a2a3a !important; font-family: Georgia,serif !important; font-size: clamp(48px,5vw,68px) !important; font-weight: 400 !important; letter-spacing: -.055em !important; line-height: .9 !important; }.letter-content h2 em { color: #b84e70; font-weight: 400; }.wish-list { min-height: 165px; color: #765667; font-family: Georgia,serif; font-size: 12.5px; line-height: 1.68; }.wish-list p { margin: 0 0 12px; animation: text-in .65s ease both; }.typing-dot { display: inline-block; width: 5px; height: 5px; border-radius: 50%; background: #bd5877; animation: blink 1s infinite; }.signature { margin-top: 17px; color: #a27a87; font-family: Georgia,serif; font-size: 12px; font-style: italic; }.signature span { margin-left: 5px; color: #cb6684; font-size: 18px; }
.card-bouquet { position: absolute; z-index: 2; right: -91px; bottom: -84px; width: 400px; opacity: .87; filter: saturate(.85) drop-shadow(0 19px 20px rgba(99,37,61,.13)); transform: rotate(-5deg); }.card-line { position: absolute; z-index: 1; right: 165px; top: 88px; width: 1px; height: 320px; background: linear-gradient(transparent,rgba(174,100,123,.18),transparent); }.card-flower-mark { position: absolute; z-index: 4; right: 28px; top: 25px; color: #c85d7e; font-size: 20px; }
.bonus-wish { display: flex; align-items: center; justify-content: center; gap: 12px; max-width: calc(100% - 40px); margin-top: 15px; color: rgba(255,246,237,.65); font-family: Georgia,serif; font-size: 11px; font-style: italic; text-align: center; }.bonus-wish span { color: var(--gold); font-style: normal; }
.replay-button { display: flex; align-items: center; gap: 7px; margin-top: 15px; border: 0; padding: 6px 10px; color: rgba(255,255,255,.38); background: transparent; font-size: 8px; font-weight: 700; letter-spacing: .14em; text-transform: uppercase; cursor: pointer; transition: .25s ease; }.replay-button span { color: var(--gold); font-size: 14px; }.replay-button:hover { color: rgba(255,255,255,.8); }

.footer { z-index: 8; height: 60px; color: rgba(255,255,255,.29); font-size: 7px; font-weight: 700; letter-spacing: .15em; text-transform: uppercase; }.footer b { color: var(--rose); font-size: 12px; }.steps { display: flex; gap: 7px; }.steps i { display: block; width: 4px; height: 4px; border-radius: 99px; background: rgba(255,255,255,.18); transition: .5s ease; }.steps i.active { width: 22px; background: var(--gold); box-shadow: 0 0 9px rgba(242,210,143,.35); }
.heart { position: absolute; color: #ff91ae; font-size: var(--size); text-shadow: 0 0 18px rgba(255,111,158,.5); animation: heart-rise var(--duration) ease-out var(--delay) forwards; }.confetti { position: absolute; width: 6px; height: 11px; border-radius: 2px; animation: confetti-burst var(--duration) cubic-bezier(.15,.75,.35,1) var(--delay) forwards; }
.scene-enter-active,.scene-leave-active { transition: opacity .75s ease,transform .75s ease,filter .75s ease; }.scene-enter { opacity: 0; filter: blur(10px); transform: translateY(18px) scale(.97); }.scene-leave-to { opacity: 0; filter: blur(10px); transform: translateY(-14px) scale(.98); }.bonus-enter-active,.bonus-leave-active,.sign-enter-active,.sign-leave-active { transition: .6s ease; }.bonus-enter,.bonus-leave-to,.sign-enter,.sign-leave-to { opacity: 0; transform: translateY(8px); }

@keyframes drift { 0%,100%{transform:translate3d(0,0,0) scale(1)}50%{transform:translate3d(-30px,35px,0) scale(1.12)} }
@keyframes twinkle { 0%,100%{opacity:.12;transform:scale(.7)}50%{opacity:.9;transform:scale(1.5)} }
@keyframes petal-fall { 0%{opacity:0;transform:translate3d(0,-25px,0) rotate(0) scale(var(--scale))}12%{opacity:.7}100%{opacity:0;transform:translate3d(var(--drift),110vh,0) rotate(540deg) scale(var(--scale))} }
@keyframes rotate { to{transform:rotate(360deg)} }
@keyframes glow-pulse { 0%,100%{opacity:.7;transform:scale(.96)}50%{opacity:1;transform:scale(1.04)} }
@keyframes envelope-float { 0%,100%{translate:0 0}50%{translate:0 -9px} }
@keyframes sparkle { 0%,100%{opacity:.2;transform:scale(.7) rotate(0)}50%{opacity:1;transform:scale(1.3) rotate(45deg)} }
@keyframes auto-progress { from{transform:scaleX(0)}to{transform:scaleX(1)} }
@keyframes bouquet-in { from{opacity:0;transform:translateY(40px) scale(.72);filter:blur(10px)}to{opacity:1;transform:none;filter:drop-shadow(0 35px 35px rgba(0,0,0,.28))} }
@keyframes bouquet-float { 0%,100%{translate:0 0}50%{translate:0 -8px} }
@keyframes text-in { from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none} }
@keyframes blink { 50%{opacity:.2} }
@keyframes equalize { to{transform:scaleY(.4)} }
@keyframes heart-rise { 0%{opacity:0;transform:translate(0,0) scale(.55) rotate(0)}12%{opacity:1}100%{opacity:0;transform:translate(var(--x),-75vh) scale(1.3) rotate(40deg)} }
@keyframes confetti-burst { 0%{opacity:1;transform:translate(0,0) rotate(0)}100%{opacity:0;transform:translate(var(--x),var(--y)) rotate(var(--rotate))} }

@media (max-width: 760px) {
  .celebration { overflow-y: auto; background: radial-gradient(circle at 50% 37%,#582448 0,#2a1129 36%,#140918 75%,#0e060f 100%); }.celebration:before { inset: 8px; border-radius: 22px; }.topbar,.footer { width: calc(100% - 42px); }.topbar { height: 68px; }.brand b { display: none; }.brand > span { width: 27px; height: 27px; }.topbar-right { gap: 12px; }.scene-count { font-size: 10px; }.music-button { width: 34px; height: 34px; }.stage { width: calc(100% - 28px); min-height: calc(100svh - 118px); }
  .intro-scene { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: calc(100svh - 145px); }.intro-copy { text-align: center; }.intro-copy .eyebrow { justify-content: center; }.intro-copy h1 { margin: 13px 0 13px !important; font-size: clamp(41px,12vw,54px) !important; line-height: .96 !important; }.intro-copy p { font-size: 11.5px; }.envelope-stage { width: 100%; height: 345px; margin-top: 1px; }.envelope-glow { width: 285px; height: 285px; }.orbit { width: 275px; height: 275px; }.orbit-two { width: 225px; height: 225px; }.envelope { width: 250px; height: 161px; }.letter-preview { left: 27px; top: 15px; width: 197px; height: 132px; padding-top: 20px; }.letter-preview b { max-width: 166px; font-size: 20px; }.wax-seal { left: calc(50% - 20px); bottom: 39px; width: 40px; height: 40px; }.envelope-open .letter-preview { transform: translateY(-81px); }.spark-one { top: 50px; right: 25px; }.spark-two { bottom: 53px; left: 29px; }.auto-note { width: 175px; margin: -6px auto 0; text-align: center; }
  .flowers-scene { justify-content: center; min-height: calc(100svh - 145px); padding: 0; }.flower-heading h2 { margin: 8px 0 -31px !important; font-size: clamp(42px,12vw,54px) !important; }.eyebrow { font-size: 7px; letter-spacing: .18em; }.eyebrow i { width: 22px; }.bouquet-stage { width: 340px; height: 405px; }.bouquet-stage img { width: 330px; }.bouquet-aura { width: 260px; height: 260px; }.bouquet-ring { width: 280px; height: 280px; }.ring-two { width: 225px; height: 225px; }.shine-one { left: 10px; top: 108px; }.shine-two { right: 13px; top: 82px; }.shine-three { right: 17px; bottom: 82px; }.flower-note { margin: -25px 0 11px; padding: 0 14px; font-size: 11.5px; }.flower-progress { align-self: center; margin: 0; text-align: center; }
  .letter-scene { justify-content: center; min-height: calc(100svh - 145px); padding: 6px 0 18px; }.letter-card { width: calc(100% - 8px); min-height: 585px; border-radius: 3px 27px 3px 27px; }.letter-card:before { inset: 8px; border-radius: 2px 20px 2px 20px; }.letter-head { padding: 25px 24px 14px; font-size: 7px; }.letter-head span:last-child { display: none; }.letter-content { width: 100%; padding: 8px 25px 33px; }.recipient { font-size: 15px; }.letter-content h2 { margin: 11px 0 21px !important; font-size: clamp(44px,12.8vw,56px) !important; }.wish-list { position: relative; z-index: 3; min-height: 212px; max-width: 92%; font-size: 12px; line-height: 1.65; }.wish-list p { margin-bottom: 11px; }.signature { position: relative; z-index: 3; margin-top: 13px; font-size: 11px; }.card-bouquet { right: -74px; bottom: -73px; width: 265px; opacity: .2; }.card-line { display: none; }.card-flower-mark { right: 22px; top: 21px; }.bonus-wish { margin-top: 12px; font-size: 10px; }.replay-button { margin-top: 10px; }.footer { height: 50px; }
}
@media (max-width: 390px) {
  .stage { min-height: calc(100svh - 112px); }.intro-scene,.flowers-scene,.letter-scene { min-height: calc(100svh - 130px); }.intro-copy h1 { font-size: 41px !important; }.envelope-stage { height: 320px; }.bouquet-stage { height: 370px; }.bouquet-stage img { width: 300px; }.letter-card { min-height: 570px; }.letter-content { padding-left: 22px; padding-right: 22px; }.letter-content h2 { font-size: 43px !important; }.wish-list { font-size: 11.7px; }
}
@media (max-width: 760px) {
  .intro-scene { display: flex; min-height: calc(100svh - 142px); justify-content: center; }
  .intro-layout { display: flex; flex-direction: column; gap: 0; min-height: 0; }
  .intro-copy { width: 100%; text-align: center; }
  .intro-copy .eyebrow { justify-content: center; }
  .date-monogram { justify-content: center; gap: 12px; margin: 0 0 17px; }
  .date-monogram b { font-size: 59px; }
  .date-monogram i { height: 43px; }
  .intro-copy h1 { margin: 12px 0 11px !important; font-size: clamp(38px,10.8vw,47px) !important; line-height: .95 !important; }
  .intro-copy p { max-width: none; padding: 0 14px; font-size: 11.5px; }
  .envelope-stage { flex: 0 0 305px; width: 100%; height: 305px; margin: 58px 0 0; }
  .intro-progress { width: 190px; margin: 2px 0 0; }
  .auto-note { align-self: center; }

  .flowers-scene { min-height: calc(100svh - 142px); justify-content: center; padding: 0 0 10px; }
  .flower-layout { display: flex; flex-direction: column; gap: 0; min-height: 0; }
  .flower-copy { order: 1; width: 100%; text-align: center; }
  .flower-copy .eyebrow { justify-content: center; }
  .flower-kicker { margin: 11px 0 7px; font-size: 7px; }
  .flower-copy h2 { font-size: clamp(38px,10.5vw,46px) !important; line-height: .89 !important; }
  .flower-copy h2 span { font-size: .57em; }
  .flower-message { justify-content: center; width: min(320px,calc(100% - 24px)); margin: 14px auto 0; padding: 9px 12px; text-align: left; }
  .flower-message > span { font-size: 25px; }
  .flower-message p { font-size: 10.5px; line-height: 1.5; }
  .bouquet-stage { order: 2; width: 100%; height: 325px; }
  .bouquet-stage img { width: 288px; }
  .bouquet-aura { width: 235px; height: 235px; }
  .bouquet-ring { width: 245px; height: 245px; }
  .ring-two { width: 198px; height: 198px; }
  .shine-one { left: 9px; top: 88px; }.shine-two { right: 12px; top: 62px; }.shine-three { right: 16px; bottom: 66px; }
  .flower-progress { width: 180px; margin: 1px 0 0; }
}
@media (max-width: 390px) {
  .date-monogram { margin-bottom: 13px; }.date-monogram b { font-size: 52px; }.intro-copy h1 { font-size: 38px !important; }
  .envelope-stage { flex-basis: 285px; height: 285px; }.flowers-scene { padding-top: 2px; }.bouquet-stage { height: 292px; }.bouquet-stage img { width: 260px; }
  .flower-copy h2 { font-size: 37px !important; }.flower-message { margin-top: 11px; }
}
@media (prefers-reduced-motion: reduce) { *,*:before,*:after { animation-duration:.01ms !important; animation-iteration-count:1 !important; transition-duration:.01ms !important; scroll-behavior:auto !important; } }
</style>
