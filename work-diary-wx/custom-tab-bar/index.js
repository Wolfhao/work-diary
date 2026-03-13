import Toast from 'tdesign-miniprogram/toast/index';

Component({
  data: {
    active: 0,
    list: [
      {
        icon: 'home',
        text: '看板',
        url: '/pages/home/index',
      },
      {
        icon: 'view-list',
        text: '商单',
        url: '/pages/order/index',
      },
      {
        icon: 'user',
        text: '我的',
        url: '/pages/my/index',
      },
    ],
  },
  methods: {
    onChange(event) {
      if (typeof this.getTabBar === 'function' && this.getTabBar()) {
        const index = event.detail.value;
        const currentData = this.data.list[index];


        this.setData({ active: index });

        wx.switchTab({
          url: currentData.url,
        });
      }
    },
    init() {
      const page = getCurrentPages().pop();
      const route = page ? page.route : '';
      const active = this.data.list.findIndex(
        (item) => (item.url.startsWith('/') ? item.url.substr(1) : item.url) === `${route}`,
      );
      this.setData({ active });
    },
  },
});
