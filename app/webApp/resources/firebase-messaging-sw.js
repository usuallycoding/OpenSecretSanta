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
        data: { groupId: data.groupId || '', category: data.category || '', target: data.target || '' }
    });
});

// The page a notification opens, by its kind: a question is answered on the Receiver page, a letter is read in the inbox, an answer and a wishlist are read
// on the Gifter page, an announcement the owner sent (the target 'messages', which the announcements kind alone does not say) is read on the
// announcements page, and anything else opens the group. The app has the same mapping (NotificationTarget); these are its addresses.
function addressOf(groupId, category, target) {
    if (!groupId) return '#/groups';
    const group = '#/groups/' + groupId;
    switch (category) {
        case 'questions': return group + '/receiver/questions';
        case 'answers': return group + '/giver/questions';
        case 'wishlist': return group + '/giver';
        case 'letters': return group + '/inbox';
        case 'announcements': return target === 'messages' ? group + '/announcements' : group;
        default: return group;
    }
}

// A new version of this worker takes over at once, so that a tap is never handled by an older copy of the code below, and
// pages that were open before it existed are controlled by it.
self.addEventListener('install', function () { self.skipWaiting(); });
self.addEventListener('activate', function (event) { event.waitUntil(self.clients.claim()); });

self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    const notified = event.notification.data || {};
    // the app's addresses are after the '#'
    const address = addressOf(notified.groupId, notified.category, notified.target);
    const target = new URL(self.registration.scope);
    target.hash = address;
    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (windows) {
            for (const page of windows) {
                if (page.url.startsWith(self.registration.scope)) {
                    // The page moves itself (push.js): WindowClient.navigate() only works on a page this worker controls, and a
                    // page opened before the worker was installed is not one.
                    page.postMessage({ type: 'oss-open', address: address });
                    return page.focus().catch(function () { return page; });
                }
            }
            return self.clients.openWindow(target.href);
        })
    );
});
