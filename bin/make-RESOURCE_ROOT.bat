mkdir RESOURCE_ROOT
cd RESOURCE_ROOT

mkdir shared-assemblies
pushd shared-assemblies
echo Now you would do something like this:
echo cvs -d /c/temp/cvsroot.lar co  -d dynamide-lib dynamide/src/assemblies/dynamide-lib
popd

mkdir homes
cd homes
echo Now you should do this:
echo cvs -d /c/temp/cvsroot.lar co -p dynamide/src/assemblies/web-apps.xml ^> web-apps.xml

mkdir dynamide
cd dynamide
echo Now you would do something like this:
echo cvs -d /c/temp/cvsroot.lar co  -d dynamide-apps dynamide/src/assemblies/dynamide-apps