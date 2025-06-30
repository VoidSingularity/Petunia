from project_template import *
import project_template as pt

# Transform using AT
ext_root = '.cache/extlibs/'
def at_transform (projectjson, properties):
    cmd = [
        pt.java, '-cp',
        os.pathsep.join (['.cache/berry/builtins.jar', '.cache/berry/loader.jar']),
        'berry.api.asm.AccessTransformer', 'manifest/berry.at'
    ]
    for jar in os.listdir (ext_root):
        cmd1 = cmd.copy ()
        cmd1.extend ([f'{ext_root}{jar}', f'extralibs/{jar}'])
        pt.syswrap (cmd1)

def getpaths ():
    return (
        ['.cache/client.jar', '.cache/server/server.jar'],
        ['.cache/bundled/', '.cache/berry/', '.cache/extramods/', 'extralibs/', 'libs/']
    )
pt.getpaths = getpaths
