call .dynamide.local.sh
pushd $DYNAMIDE_HOME
call $ANT_HOME/bin/ant $*
popd