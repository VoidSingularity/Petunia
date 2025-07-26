from project_template import *
import project_template as pt

# Shadow using ShadowClass, then transform using AT
proj_shadow = json.load (open ('project.json')) ['shadows']
def shadow (name, url):
    shadow = proj_shadow [name]
    fr, tr = shadow ['from'], shadow ['to']
    cmd = [
        pt.java, '-cp',
        os.pathsep.join (['.cache/berry/builtins.jar', '.cache/berry/loader.jar']),
        'berry.api.asm.ShadowClass', fr, tr, f'.cache/runtime/{url.split("/")[-1]}', '.cache/__shadow_temp.jar'
    ]
    pt.syswrap (cmd)
    cmd = [
        pt.java, '-cp',
        os.pathsep.join (['.cache/berry/builtins.jar', '.cache/berry/loader.jar']),
        'berry.api.asm.AccessTransformer', 'manifest/berry.at',
        '.cache/__shadow_temp.jar', f'runtime/{name}-{url.split("/")[-1]}'
    ]
    pt.syswrap (cmd)
pt.processors ['mixin'] = shadow
pt.processors ['mextras'] = shadow

class GLF:
    def __init__ (self): self.flag = False
    def true (self): self.flag = True

pel_flag = GLF ()

def ater (src, dst):
    pel_flag.true ()
    cmd = [
        pt.java, '-cp',
        os.pathsep.join (['.cache/berry/builtins.jar', '.cache/berry/loader.jar']),
        'berry.api.asm.AccessTransformer', 'manifest/berry.at', src, dst
    ]
    pt.syswrap (cmd)
el_fabric = pt.external_library_parser ('el_fabric', ater)

def shadower (shadowcfg):
    shadowsrc, shadowdst = shadowcfg ['from'], shadowcfg ['to']
    def shadow (src, dst):
        pel_flag.true ()
        cmd = [
            pt.java, '-cp',
            os.pathsep.join (['.cache/berry/builtins.jar', '.cache/berry/loader.jar']),
            'berry.api.asm.ShadowClass', shadowsrc, shadowdst, src, '.cache/__shadow_temp.jar'
        ]
        pt.syswrap (cmd)
        cmd = [
            pt.java, '-cp',
            os.pathsep.join (['.cache/berry/builtins.jar', '.cache/berry/loader.jar']),
            'berry.api.asm.AccessTransformer', 'manifest/berry.at',
            '.cache/__shadow_temp.jar', dst
        ]
        pt.syswrap (cmd)
    return shadow
el_forge = pt.external_library_parser ('el_forge', shadower (proj_shadow ['forge']))
el_neoforge = pt.external_library_parser ('el_neoforge', shadower (proj_shadow ['neoforge']))

def pel_clean (*_):
    if not pel_flag.flag and os.path.exists ('.cache/extlibs'): shutil.rmtree ('.cache/extlibs')
    if not os.path.exists ('.cache/extlibs'): os.mkdir ('.cache/extlibs')

def shadowinfo (projectjson, properties):
    f = open ('src/main/berry/unify/ShadowConfigGenerated.java', 'w')
    f.write ('package berry.unify;\n\nclass ShadowConfigGenerated {\n    record Info(String from, String to){}\n')
    for k in proj_shadow: f.write (f'    static Info {k} = new Info("{proj_shadow[k]["from"]}", "{proj_shadow[k]["to"]}");\n')
    f.write ('}\n'); f.close ()

def main (projectjson, properties):
    njar = zipfile.ZipFile ('output/main.jar', 'w')
    def pjar (jar: zipfile.ZipFile):
        for info in jar.filelist:
            name = info.filename
            try: njar.getinfo (name)
            except KeyError:
                os = njar.open (name, 'w')
                os.write (jar.open (name) .read ())
                os.close ()
        jar.close ()
    def qjar (loc): pjar (zipfile.ZipFile (loc))
    qjar ('output/main_raw.jar')
    for i in os.listdir ('runtime/'): qjar (f'runtime/{i}')
    njar.close ()

# javap
def javap (side, cls):
    match side.lower ():
        case 'client': path = '.cache/client.jar'
        case 'server': path = '.cache/server/server.jar'
        case _:
            print ('Invalid side:', side)
            return
    zf = zipfile.ZipFile (path, 'r')
    of = open ('.cache/javap_temp.class', 'wb')
    zpath = cls.replace ('.', '/') + '.class'
    of.write (zf.open (zpath) .read ())
    of.close ()
    zf.close ()
    syswrap ([ pt.java + 'p', '-c', '-s', '-p', '.cache/javap_temp.class' ])

if __name__ == '__main__':
    if len (sys.argv) > 1:
        match sys.argv [1]:
            case 'javap':
                if len (sys.argv) >= 3:
                    for cls in sys.argv [3:]:
                        javap (sys.argv [2], cls)
            case _: pass
