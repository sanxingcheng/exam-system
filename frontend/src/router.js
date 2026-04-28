import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from './stores'
import LoginView from './views/LoginView.vue'
import ChangePasswordView from './views/ChangePasswordView.vue'
import BanksView from './views/BanksView.vue'
import QuestionsView from './views/QuestionsView.vue'
import AdminView from './views/AdminView.vue'
import ExamView from './views/ExamView.vue'
import ScoresView from './views/ScoresView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/change-password', component: ChangePasswordView },
    { path: '/', component: BanksView },
    { path: '/questions', component: QuestionsView },
    { path: '/admin', component: AdminView },
    { path: '/exam', component: ExamView },
    { path: '/scores', component: ScoresView },
  ],
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (to.path !== '/login' && !session.loggedIn) {
    return '/login'
  }
  if (session.passwordChangeRequired && to.path !== '/change-password' && to.path !== '/login') {
    return '/change-password'
  }
  if (to.path === '/admin' && !session.isAdmin) {
    return '/'
  }
  return true
})

export default router
