#/bin/bash

echo '{"kind":"ServiceAccount","apiVersion":"v1","metadata":{"name":"kubeless"}}' | oc create -f -
oc policy add-role-to-user edit system:serviceaccount:kubeless:kubeless

