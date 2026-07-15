# Steam bounce page (gameslibrary.kotwave.hr)

`callback/index.html` serves `https://gameslibrary.kotwave.hr/callback` — the OpenID
return_to that forwards Steam's callback to the app's loopback listener.

Deploy (Cloudflare Pages project `gameslibrary`, direct upload): drag the `callback`
**folder** into Create deployment. The upload widget keeps the dragged folder as a path
segment, so the folder name is the route.
