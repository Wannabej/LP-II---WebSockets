#include <bits/stdc++.h>
#define pb push_back
#define inic ios_base::sync_with_stdio(false);
#define inic2 cin.tie(NULL);
#define inic3 cout.tie(NULL);
#define all(s) s.begin(), s.end()
using namespace std;
typedef long long ll;
#define Rep0(i, n) for (ll i = 0; i < (ll)n; i++)
#define Rep1(i, n) for (ll i = 1; i <= (ll)n; i++)
#define Repi0(i, n) for (ll i = n - 1; i >= 0; i--)
#define Repi1(i, n) for (ll i = n; i >= 1; i--)
#define POT(x) ((x) * (x))
const ll MX = 1e5 + 5;
const ll MOD = 998244353;
const ll INF = 1e18;
const long double INF_DOUBLE = 1e18 / 1.0;
const long double EPS = 1e-8;
const long double PI = acos(-1.0);
typedef long double ld;
typedef unsigned long long ull;

ull getBit(ull x, ull i) {
    return (x >> i) & 1;
}

ll f(ll i, ll n, ll k){
    return abs(i*i+(2*k-1)*i-(n*k+n*(n-1)/2));
}

void solve(){
    ll n, k; cin >> n >> k;
    ll low = 1, high = n;
    while(high - low > 2){
        ll m1 = low + (high - low)/3;
        ll m2 = high - (high - low)/3;
        if (f(m1, n, k) > f(m2, n, k)) low = m1;
        else high = m2;
    }
    ll mini = f(low, n, k);
    for(ll i = low+1; i<=high; i++){
        mini = min(mini, f(i, n, k));
    }
    cout << mini << "\n";
}

int main() {
    inic;
    inic2;
    int t; cin>>t;
    while(t--){
    solve();
    }
    return 0;
}