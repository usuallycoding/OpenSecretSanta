// The service worker that shows Open Secret Santa's push notifications while no page of the app is in front, and opens the
// app on the right group when one is tapped.
//
// It holds no settings of its own: push.js registers it with the public Firebase web settings in its address, because a
// service worker must set up its handlers while it is first read and so cannot fetch config.json first.
importScripts(
    'https://www.gstatic.com/firebasejs/11.0.2/firebase-app-compat.js',
    'https://www.gstatic.com/firebasejs/11.0.2/firebase-messaging-compat.js'
);

const settings = new URL(self.location.href).searchParams;
firebase.initializeApp({
    apiKey: settings.get('apiKey'),
    projectId: settings.get('projectId'),
    messagingSenderId: settings.get('messagingSenderId'),
    appId: settings.get('appId')
});

// The server sends data only: the title and the text are decided there and say nothing private.
firebase.messaging().onBackgroundMessage(function (payload) {
    const data = payload.data || {};
    if (!data.body) return;
    return self.registration.showNotification(data.title || 'Open Secret Santa', {
        body: data.body,
        // a newer notification of a kind for a group replaces the older one
        tag: (data.category || 'message') + ':' + (data.groupId || ''),
        data: { groupId: data.groupId || '' }
    });
});

self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    const groupId = event.notification.data && event.notification.data.groupId;
    // the app's addresses are after the '#'
    const target = new URL(self.registration.scope);
    target.hash = groupId ? '#/groups/' + groupId : '#/groups';
    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (windows) {
            for (const page of windows) {
                if (page.url.startsWith(self.registration.scope)) {
                    return page.focus().then(function (focused) {
                        return focused && focused.navigate ? focused.navigate(target.href) : focused;
                    });
                }
            }
            return self.clients.openWindow(target.href);
        })
    );
});
