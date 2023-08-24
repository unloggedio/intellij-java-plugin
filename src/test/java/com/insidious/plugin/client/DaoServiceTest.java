package com.insidious.plugin.client;

import com.insidious.plugin.pojo.dao.Parameter;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DaoServiceTest {

    @Test
    public void getMethodCallExpressionToMockFast() throws SQLException {
//        ConnectionSource connectionSource = new JdbcConnectionSource(
//                "jdbc:sqlite:/Users/artpar/workspace/code/insidious/plugin/execution.db");
//        DaoService daoService = new DaoService(connectionSource, value -> null);

        String[] callListIdArray = ("11306,11307,11308,11309,11310,11311,11312,11313,11315,11316,11317,11318,11319,11320,11321,11322,11323,11325,11326,11327,11328,11329,11330,11331,11332,11333,11334,11335,11336,11337,11338,11339,11340,11341,11342,11343,11344,11347,11348,11349,11352,11353,11354,11355,11356,11357,11358,11359,11360,11361,11362,11363,11366,11367,11368,11374,11375,11376,11378,11379,11380,11381,11382,11383,11384,11385,11386,11387,11388,11389,11390,11391,11392,11393,11394,11395,11396,11397,11398,11399,11400,11401,11402,11404,11405,11407,11408,11409,11410,11412,11413,11415,11416,11418,11419,11421,11422,11423,11424,11425,11426,11427,11428,11429,11432,11433,11434,11435,11436,11437,11438,11439,11440,11443,11445,11446,11447,11448,11449,11450,11451,11452,11454,11455,11456,11457,11458,11459,11460,11461,11462,11463,11465,11466,11467,11468,11469,11470,11471,11472,11473,11474,11475,11476,11477,11478,11479,11480,11481,11482,11483,11485,11486,11487,11488,11489,11490,11491,11492,11493,11494,11495,11496,11497,11498,11499,11500,11501,11502,11503,11504,11505,11506,11507,11508,11509,11510,11511,11512,11513,11514,11515,11516,11517,11518,11519,11520,11521,11522,11523,11524,11525,11526,11527,11528,11529,11530,11531,11532,11533,11534,11535,11536,11537,11538,11539,11540,11541,11542,11543,11545,11546,11547,11548,11549,11550,11551,11552,11553,11554,11556,11557,11558,11559,11560,11561,11562,11563,11564,11565,11566,11567,11568,11569,11570,11571,11572,11573,11574,11576,11577,11578,11579,11580,11581,11582,11583,11584,11585,11586,11587,11588,11589,11590,11591,11592,11593,11594,11595,11596,11597,11598,11599,11600,11601,11602,11603,11604,11605,11606,11607,11608,11609,11610,11611,11612,11613,11614,11615,11616,11617,11618,11619,11620,11621,11622,11623,11624,11625,11626,11627,11628,11629,11630,11631,11632,11633,11634,11635,11636,11637,11638,11639,11642,11643,11644,11645,11646,11647,11648,11649,11650,11651,11652,11653,11654,11655,11656,11657,11658,11659,11660,11661,11662,11663,11664,11665,11666,11667,11668,11669,11670,11671,11672,11673,11674,11675,11677,11678,11679,11680,11682,11683,11684,11685,11686,11687,11688,11689,11690,11691,11693,11694,11695,11696,11697,11698,11699,11700,11701,11702,11703,11704,11705,11706,11707,11708,11709,11710,11711,11713,11714,11715,11716,11717,11718,11719,11720,11721,11722,11723,11724,11725,11726,11727,11728,11729,11730,11731,11732,11733,11734,11735,11736,11737,11738,11739,11740,11741,11742,11743,11744,11745,11746,11747,11748,11749,11750,11751,11752,11753,11754,11755,11756,11757,11758,11759,11760,11761,11762,11763,11764,11765,11766,11767,11768,11769,11770,11772,11773,11774,11775,11776,11777,11778,11779,11780,11781,11782,11783,11784,11785,11786,11787,11788,11789,11790,11791,11792,11793,11794,11795,11796,11797,11798,11799,11800,11801,11802,11803,11804,11805,11806,11807,11808,11809,11810,11811,11812,11813,11814,11815,11817,11818,11819,11820,11821,11822,11823,11824,11825,11826,11828,11829,11830,11831,11832,11833,11834,11835,11836,11837,11838,11839,11840,11841,11842,11843,11844,11845,11846,11848,11849,11850,11851,11852,11853,11854,11855,11856,11857,11858,11859,11860,11861,11862,11863,11864,11865,11866,11867,11868,11869,11870,11871,11872,11873,11874,11875,11876,11877,11878,11879,11880,11881,11882,11883,11884,11885,11886,11887,11888,11889,11890,11891,11892,11893,11894,11895,11896,11897,11898,11899,11900,11901,11902,11903,11904,11905,11907,11908,11909,11910,11911,11912,11913,11914,11915,11916,11917,11918,11919,11920,11921,11922,11923,11924,11925,11926,11927,11928,11929,11930,11931,11932,11933,11934,11935,11936,11937,11938,11939,11940,11941,11942,11943,11944,11945,11946,11947,11948,11949,11950,11951,11952,11953,11954,11955,11956,11957,11960,11961,11962,11963,11964,11965,11966,11967,11968,11969,11970,11971,11972,11975,11976,11977,11978,11979,11980,11981,11982,11983,11984,11985,11986,11987,11988,11989,11990,11991,11992,11993,11994,11995,11996,11997,11998,12000,12001,12002,12003,12004,12005,12006,12007,12008,12009,12010,12011,12012,12013,12014,12015,12016,12017,12018,12019,12020,12021,12022,12023,12024,12025,12026,12027,12028,12029,12030,12031,12032,12033,12034,12035,12036,12037,12038,12039,12040,12041,12042,12043,12044,12045,12046,12047,12048,12049,12050,12051,12052,12053,12054,12057,12058,12059,12060,12061,12062,12065,12066,12067,12068,12069,12070,12071,12072,12073,12074,12075,12076,12077,12078,12079,12080,12087,12090,12091,12092,12093,12094,12095,12096,12097,12099,12100,12101,12102,12103,12104,12105,12106,12107,12108,12109,12110,12111,12112,12113,12114,12115,12116,12117,12118,12119,12120,12121,12122,12123,12124,12125,12127,12128,12130,12131,12133,12134,12135,12136,12137,12138,12139,12140,12141,12142,12143,12147,12148,12149,12150,12151,12152,12153,12154,12155,12156,12157,12158,12159,12160,12161,12162,12163,12166,12167,12168,12169,12170,12171,12172,12173,12174,12175,12178,12179,12180,12181,12182,12183,12184,12187,12188,12189,12190,12191,12192,12193,12194,12195,12196,12197,12198,12199,12200,12201,12202,12203,12204,12205,12206,12207,12209,12210,12211,12212,12213,12214,12216,12217,12218,12219,12222,12223,12224,12225,12226,12227,12228,12229,12230,12231,12232,12233,12234,12235,12236,12237,12238,12239,12240,12241,12242,12243,12244,12248,12249,12250,12251,12252,12253,12254,12255,12256,12257,12258,12259,12260,12261,12262,12263,12264,12265,12266,12267,12268,12269,12270,12271,12272,12273,12274,12275,12276,12278,12279,12280,12281,12282,12283,12284,12285,12286,12287,12288,12289,12290,12291,12292,12293,12294,12295,12296,12297,12298,12299,12300,12301,12302,12303,12304,12305,12306,12307,12308,12309,12311,12312,12314,12315,12317,12318,12319,12320,12321,12322,12323,12324,12325,12326,12327,12328,12329,12330,12332,12333,12334,12335,12336,12337,12338,12339,12340,12341,12342,12343,12344,12345,12346,12347,12348,12349,12350,12351,12352,12353,12354,12355,12356,12357,12358,12359,12360,12361,12362,12363,12364,12365,12366,12367,12368,12369,12370,12371,12372,12373,12374,12375,12376,12377,12378,12379,12380,12381,12383,12384,12385,12386,12387,12388,12389,12390,12391,12392,12394,12395,12396,12397,12398,12399,12400,12401,12402,12403,12404,12405,12406,12407,12408,12409,12410,12411,12412,12414,12415,12416,12417,12418,12419,12420,12421,12422,12423,12424,12425,12426,12427,12428,12429,12430,12431,12432,12433,12434,12435,12436,12437,12438,12439,12440,12441,12442,12443,12444,12445,12446,12447,12448,12449,12450,12451,12452,12453,12454,12455,12456,12457,12458,12459,12460,12461,12462,12463,12464,12465,12466,12467,12468,12469,12470,12471,12473,12474,12475,12476,12477,12478,12479,12480,12481,12482,12483,12484,12485,12486,12487,12488,12489,12490,12491,12492,12493,12494,12495,12496,12497,12498,12499,12500,12501,12502,12503,12504,12505,12506,12507,12508,12509,12510,12511,12512,12513,12514,12530,12532,12534,12535,12536,12537,12538,12539,12540,12541,12542,12543,12545,12546,12547,12548,12549,12550,12551,12552,12555,12556,12557,12558,12559,12560,12561,12562,12565,12566,12567,12568,12569,12570,12571,12572,12574,12575,12576,12577,12578,12579,12580,12581,12582,12583,12584,12585,12586,12587,12588,12589,12590,12591,12592,12593,12594,12595,12596,12597,12598,12599,12600,12601,12602,12603,12604,12605,12606,12607,12608,12609,12611,12612,12613,12614,12615,12616,12617,12618,12619,12620,12622,12623,12624,12625,12626,12627,12628,12629,12630,12631,12632,12633,12634,12635,12636,12637,12638,12639,12640,12642,12643,12644,12645,12646,12647,12648,12649,12650,12651,12652,12653,12654,12655,12656,12657,12658,12659,12660,12661,12662,12663,12664,12665,12666,12667,12668,12669,12670,12671,12672,12673,12674,12675,12676,12677,12678,12679,12680,12681,12682,12683,12684,12685,12686,12687,12688,12689,12690,12691,12692,12693,12694,12695,12696,12697,12698,12699,12701,12702,12703,12704,12705,12706,12707,12708,12709,12710,12711,12712,12713,12714,12715,12716,12717,12718,12719,12721,12722,12723,12724,12725,12726,12727,12728,12729,12730,12731,12732,12733,12734,12735,12736,12737,12738,12739,12740,12741,12742,12743,12744,12745,12746,12747,12748,12749,12750,12751,12752,12753,12754,12755,12756,12757,12758,12759,12761,12762,12763,12765,12767,12768,12769,12770,12771,12772,12773,12774,12775,12777,12778,12779,12780,12782,12783,12784,12785,12787,12788,12789,12790,12791,12792,12793,12794,12795,12796,12798,12799,12800,12801,12802,12803,12804,12805,12806,12807,12808,12809,12810,12811,12812,12813,12814,12815,12816,12818,12819,12820,12821,12822,12823,12824,12825,12826,12827,12828,12829,12830,12831,12832,12833,12834,12835,12836,12837,12838,12839,12840,12841,12842,12843,12844,12845,12846,12847,12848,12849,12850,12851,12852,12853,12854,12855,12856,12857,12858,12859,12860,12861,12862,12863,12864,12865,12866,12867,12868,12869,12870,12871,12872,12873,12874,12875,12876,12877,12878,12879,12880,12881,12890,12897,12898,12899,12900,12901,12902,12905,12906,12907,12908,12909,12910,12911,12912,12913,12914,12915,12916,12917,12918,12920,12921,12922,12923,12924,12925,12926,12927,12928,12929,12930,12931,12932,12933,12934,12935,12936,12937,12938,12939,12940,12941,12942,12943,12944,12945,12946,12947,12948,12949,12950,12951,12952,12953,12954,12955,12957,12958,12959,12960,12961,12962,12963,12964,12965,12966,12967,12968,12969,12970,12971,12972,12973,12974,12975,12976,12977,12978,12979,12980,12981,12982,12983,12985,12986,12988,12989,12991,12992,12993,12994,12995,12996,12997,12998,12999,13000,13001,13002,13003,13006,13007,13008,13011,13012,13014,13015,13016,13017,13018,13019,13020,13021,13022,13023,13024,13025,13026,13027,13028,13029,13030,13031,13032,13033,13034,13035,13036,13037,13039,13040,13041,13042,13043,13044,13045,13046,13047,13048,13049,13050,13051,13052,13053,13054,13055,13056,13057,13058,13059,13061,13062,13063,13064,13065,13066,13067,13068,13069,13072,13073,13074,13075,13076,13077,13078,13079,13082,13083,13084,13085,13086,13087,13088,13089,13091,13092,13093,13094,13095,13096,13097,13098,13099,13100,13101,13102,13103,13104,13105,13106,13107,13108,13109,13110,13111,13112,13113,13114,13116,13117,13118,13119,13120,13121,13122,13123,13124,13125,13127,13128,13129,13130,13131,13132,13133,13134,13135,13136,13137,13138,13139,13140,13141,13142,13143,13144,13145,13147,13148,13149,13150,13151,13152,13153,13154,13155,13156,13157,13158,13159,13160,13161,13162,13163,13164,13165,13166,13167,13168,13169,13170,13171,13172,13173,13174,13175,13176,13177,13178,13179,13180,13181,13182,13183,13184,13185,13186,13187,13188,13189,13190,13191,13192,13193,13194,13195,13196,13197,13198,13199,13200,13201,13202,13203,13204,13205,13206,13207,13208,13210,13211,13212,13213,13214,13215,13216,13217,13218,13219,13220,13221,13222,13223,13224,13225,13226,13227,13228,13229,13230,13231,13232,13233,13234,13235,13236,13238,13239,13241,13242,13244,13245,13246,13247,13248,13249,13250,13251,13252,13253,13254,13255,13256,13257,13258,13259,13261,13262,13263,13264,13265,13266,13267,13268,13269,13270,13271,13272,13273,13274,13275,13276,13277,13278,13279,13280,13281,13282,13283,13284,13285,13286,13287,13288,13289,13290,13291,13292,13293,13294,13295,13296,13298,13299,13301,13302,13304,13305,13306,13307,13308,13309,13310,13311,13312,13313,13314,13315,13316,13317,13318,13319,13320,13321,13322,13323,13324,13325,13326,13327,13329,13330,13331,13332,13333,13334,13335,13336,13337,13338,13339,13340,13341,13342,13343,13344,13345,13346,13347,13348,13349,13350,13351,13352,13353,13354,13355,13356,13357,13358,13359,13360,13361,13362,13363,13364,13366,13367,13369,13370,13372,13373,13374,13375,13376,13377,13378,13379,13380,13381,13382,13383,13384,13385,13386,13387,13388,13389,13390,13391,13392,13393,13394,13395,13396,13397,13398,13399,13401,13402,13403,13404,13405,13406,13407,13408,13409,13410,13411,13412,13413,13414,13415,13416,13417,13418,13419,13420,13421,13422,13423,13424,13425,13426,13427,13428,13429,13430,13432,13433,13435,13436,13438,13439,13440,13441,13442,13443,13444,13445,13446,13447,13450,13451,13452,13453,13454,13455,13456,13457,13458,13459,13460,13461,13462,13463,13464,13465,13466,13467,13468,13469,13472,13473,13474,13475,13476,13477,13478,13479,13480,13481,13482,13483,13484,13485,13486,13488,13489,13490,13491,13492,13493,13494,13495,13496,13497,13498,13499,13500,13502,13503,13504,13505,13506,13507,13508,13509,13510,13511,13512,13513,13514,13515,13516,13517,13518,13519,13520,13522,13523,13524,13525,13526,13527,13528,13529,13530,13531,13532,13533,13534,13535,13536,13537,13538,13539,13540,13541,13542,13543,13544,13545,13546,13547,13548,13549,13550,13551,13552,13553,13554,13555,13556,13557,13558,13559,13560,13561,13562,13563,13564,13565,13566,13567,13568,13569,13570,13571,13572,13573,13574,13575,13576,13577,13578,13579,13580,13581,13582,13583,13585,13586,13587,13588,13589,13590,13591,13592,13593,13594,13595,13596,13597,13598,13599,13600,13601,13602,13603,13604,13605,13606,13607,13608,13609,13610,13611,13612,13613,13614,13615,13616,13617,13618,13619,13620,13621,13622,13623,13624,13625,13626,13627,13628,13629,13630,13631,13632,13633,13634,13635,13636,13638,13639,13640,13641,13642,13643,13644,13645,13646,13647,13648,13649,13650,13651,13652,13653,13654,13655,13656,13657,13658,13659,13660,13661,13662,13663,13664,13666,13667,13669,13670,13672,13673,13674,13675,13676,13677,13678,13679,13680,13681,13682,13686,13687,13688,13689,13690,13691,13692,13693,13694,13695,13696,13697,13698,13699,13700,13701,13702,13705,13706,13707,13708,13709,13710,13711,13712,13713,13714,13717,13718,13719,13720,13721,13722,13723,13726,13727,13728,13729,13730,13731,13732,13733,13734,13735,13736,13737,13738,13739,13740,13741,13742,13743,13744,13745,13746,13748,13749,13750,13751,13752,13753,13755,13756,13757,13758,13761,13762,13763,13764,13765,13766,13767,13768,13769,13770,13771,13772,13773,13774,13775,13776,13777,13778,13779,13780,13782,13783,13784,13785,13786,13787,13788,13789,13790,13791,13792,13793,13794,13795,13796,13797,13798,13799,13800,13801,13802,13803,13804,13805,13806,13807,13809,13810,13812,13813,13815,13816,13817,13818,13819,13820,13821,13822,13823,13824,13827,13828,13829,13833,13835,13836,13837,13838,13839,13840,13841,13844,13845,13846,13847,13848,13850,13851,13852,13853,13854,13855,13856,13857,13858,13859,13860,13861,13862,13863,13864,13865,13866,13867,13868,13869,13870,13871,13872,13873,13874,13875,13876,13877,13878,13879,13880,13881,13882,13883,13885,13886,13887,13888,13889,13890,13891,13892,13893,13894,13895,13896,13897,13898,13899,13900,13901,13902,13903,13905,13906,13907,13908,13910,13911,13912,13913,13914,13915,13916,13917,13918,13919,13920,13921,13922,13923,13924,13925,13926,13927,13928,13929,13930,13931,13932,13933,13934,13935,13936,13937,13938,13939,13940,13941,13942,13944,13945,13947,13948,13950,13951,13952,13953,13954,13955,13956,13957,13958,13959,13960,13961,13962,13963,13965,13966,13967,13968,13969,13970,13972,13973,13974,13975,13976,13977,13978,13979,13980,13981,13982,13983,13984,13985,13986,13987,13988,13989,13990,13991,13992,13993,13994,13995,13996,13997,13998,13999,14000,14001,14002,14004,14005,14006,14008,14009,14010,14011,14012,14013,14015,14016,14017,14018,14019,14020,14021,14022,14023,14024,14025,14026,14027,14028,14029,14030,14031,14034,14035,14036,14037,14038,14041,14042,14043,14044,14045,14046,14047,14048,14049,14051,14052,14053,14054,14055,14056,14057,14058,14059,14060,14062,14063,14064,14065,14066,14067,14068,14069,14070,14071,14072,14073,14074,14075,14076,14077,14078,14079,14080,14082,14083,14084,14085,14086,14087,14088,14089,14090,14091,14092,14093,14094,14095,14096,14097,14098,14099,14100,14101,14102,14103,14104,14105,14106,14107,14108,14109,14110,14111,14112,14113,14114,14115,14116,14117,14118,14119,14120,14121,14122,14123,14124,14125,14126,14127,14128,14129,14130,14131,14132,14133,14134,14135,14136,14137,14138,14139,14141,14142,14143,14144,14145,14146,14147,14148,14149,14150,14151,14152,14153,14154,14155,14156,14157,14158,14159,14160,14161,14162,14163,14164,14165,14166,14167,14168,14169,14170,14171,14172,14173,14174,14175,14176,14177,14178,14179,14180,14181,14182,14183,14185,14186,14187,14188,14189,14190,14191,14192,14193,14194,14195,14196,14197,14198,14199,14200,14201,14202,14203,14204,14205,14206,14207,14208,14209,14210,14211,14212,14213,14214,14215,14217,14218,14219,14220,14221,14222,14224,14225,14226,14227,14228,14229,14230,14231,14232,14233,14234,14235,14236,14237,14238,14239,14240,14241,14242,14243,14244,14245,14246,14247,14248,14249,14250,14251,14252,14253,14256,14258,14259,14260,14261,14262,14263,14264,14266,14267,14268,14269,14271,14272,14273,14274,14276,14277,14278,14279,14281,14282,14283,14284,14286,14287,14288,14289,14291,14292,14293,14294,14296,14297,14298,14299,14301,14302,14303,14304,14306,14307,14308,14309,14312,14314,14315,14316,14317,14318,14319,14320,14322,14323,14324,14325,14327,14328,14329,14330,14332,14333,14334,14335,14337,14338,14339,14340,14342,14343,14344,14345,14347,14348,14349,14350,14352,14353,14354,14355,14357,14358,14359,14360,14362,14363,14364,14365,14368,14370,14371,14372,14373,14374,14375,14376,14378,14379,14380,14381,14383,14384,14385,14386,14388,14389,14390,14391,14393,14394,14395,14396,14398,14399,14400,14401,14403,14404,14405,14406,14408,14409,14410,14411,14413,14414,14415,14416,14418,14419,14420,14421,14422,14423,14424,14425,14426,14428,14429,14430,14431,14432,14433,14434,14435,14437,14438,14439,14440,14441,14442,14443,14444,14445,14446,14447,14448,14449,14450,14451,14452,14453,14456,14457,14458,14459,14460,14462,14463,14464,14465,14466,14467,14468,14469,14470,14472,14473,14474,14475,14476,14477,14478,14479,14481,14482,14483,14484,14485,14486,14487,14488,14489,14490,14491,14492,14493,14494,14495,14496,14497,14500,14501,14502,14503,14504,14506,14507,14508,14514,14516,14533,14535,14536,14537,14538,14539,14541,14542,14543,14544,14545,14546,14547,14548,14549,14550,14551,14552,14553,14554,14556,14557,14558,14559,14560,14561,14562,14563,14564,14565,14566,14567,14568,14569,14570,14571,14572,14573,14574,14575,14576,14577,14578,14579,14580,14581,14582,14583,14584,14585,14586,14587,14588,14589,14590,14591,14593,14594,14595,14596,14597,14598,14599,14600,14602,14603,14604,14605,14606,14607,14608,14609,14610,14611,14612,14613,14614,14615,14616,14617,14618,14621,14622,14623,14624,14625,14627,14628,14630,14631,14632,14633,14634,14635,14636,14637,14638,14639,14640,14641,14642,14643,14644,14645,14646,14647,14648,14649,14650,14651,14652,14653,14654,14655,14656,14657,14658,14659,14660,14661,14662,14663,14664,14665,14666,14667,14668,14669,14670,14671,14672,14673,14674,14675,14676,14677,14678,14679,14680,14681,14682,14683,14684,14685,14686,14687,14688,14689,14690,14691,14692,14693,14694,14695,14696,14697,14698,14699,14700,14701,14702,14703,14704,14705,14706,14707,14708,14709,14710,14711,14712,14713,14714,14715,14716,14717,14718,14719,14720,14721,14722,14723,14724,14725,14726,14727,14728,14729,14730,14731,14732,14733,14734,14735,14736,14737,14738,14739,14740,14741,14742,14743,14744,14745,14746,14747,14748,14749,14750,14751,14752,14753,14754,14755,14756,14757,14758,14759,14760,14761,14762,14763,14764,14765,14766,14767,14768,14769,14770,14771,14772,14773,14774,14775,14776,14777,14778,14779,14780,14781,14782,14783,14784,14785,14786,14787,14788,14789,14790,14791,14792,14793,14794,14795,14796,14797,14798,14799,14800,14801,14802,14803,14804,14805,14806,14807,14808,14809,14810,14811,14812,14814,14815,14816,14817,14818,14819,14821,14822,14823,14824,14825,14826,14827,14829,14830,14831,14832,14833,14834,14835,14836,14837,14838,14839,14840,14841,14842,14843,14844,14845,14847,14848,14849,14850,14851,14852,14853,14855,14856,14857,14858,14859,14860,14861,14862,14863,14864,14865,14866,14867,14868,14869,14870,14871,14872,14873,14874,14875,14876,14877,14878,14879,14880,14881,14882,14883,14884,14885,14886,14887,14888,14889,14890,14892,14893,14895,14896,14898,14899,14900,14901,14902,14903,14904,14905,14906,14907,14916,14917,14918,14919,14920,14921,14922,14923,14924,14925,14926,14927,14928,14929,14930,14931,14932,14933,14934,14935,14936,14937,14939,14940,14941,14942,14943,14944,14945,14946,14947,14948,14949,14950,14951,14952,14954,14955,14956,14958,14959,14960,14961,14962,14963,14964,14966,14967,14968,14969,14970,14971,14972,14974,14975,14976,14977,14978,14979,14980,14984,14989,14994,14999,15004,15009,15014,15019,15024,15029,15034,15039,15044,15046,15047,15048,15049,15050,15052,15053,15054,15055,15056,15057,15058,15059,15060,15062,15063,15064,15065,15066,15067,15068,15069,15070,15071,15072,15073,15074,15075,15076,15077,15078,15079,15080,15081,15083,15084,15085,15086,15087,15088,15089,15090,15091,15092,15094,15095,15096,15097,15098,15099,15100,15101,15102,15103,15104,15105,15106,15107,15108,15109,15110,15111,15112,15114,15115,15116,15117,15118,15119,15120,15121,15122,15123,15124,15125,15126,15127,15128,15129,15130,15131,15132,15133,15134,15135,15136,15137,15138,15139,15140,15141,15142,15143,15144,15145,15146,15147,15148,15149,15150,15151,15152,15153,15154,15155,15156,15157,15158,15159,15160,15161,15162,15163,15164,15165,15166,15167,15168,15169,15170,15171,15173,15175,15176,15177,15178,15179,15180,15181,15182,15183,15184,15185,15186,15187,15188,15189,15190,15192,15193,15194,15195,15196,15197,15198,15199,15200,15201,15202,15203,15204,15205,15206,15207,15208,15209,15210,15211,15212,15213,15214,15215,15216,15217,15218,15219,15221,15222,15224,15225,15227,15228,15229,15230,15231,15232,15233,15234,15235,15236,15244,15245,15246,15247,15248,15249,15251,15252,15253,15255,15256,15257,15258,15259,15260,15261,15262,15263,15264,15265,15266,15268,15269,15270,15271,15272,15273,15274,15275,15276,15277,15278,15279,15280,15281,15282,15283,15284,15285,15286,15287,15288,15289,15290,15291,15292,15293,15294,15295,15296,15297,15298,15299,15300,15301,15302,15303,15304,15305,15306,15308,15309,15311,15312,15314,15315,15316,15317,15318,15319,15320,15321,15322,15323,15331,15332,15333,15334,15335,15336,15337,15338,15339,15340,15341,15342,15343,15344,15345,15346,15347,15350,15351,15352,15355,15356,15357,15358,15359,15360,15361,15362,15363,15364,15365,15366,15367,15368,15369,15370,15371,15372,15373,15374,15376,15377,15378,15379,15380,15381,15382,15383,15384,15385,15386,15387,15388,15389,15390,15391,15392,15393,15396,15397,15398,15399,15400,15401,15402,15403,15404,15405,15407,15408,15409,15410,15411,15412,15414,15415,15416,15417,15418,15419,15420,15421,15422,15423,15424,15425,15426,15427,15428,15429,15430,15431,15432,15433,15434,15435,15436,15437,15438,15439,15440,15441,15442,15443,15444,15445,15446,15447,15448,15449,15450,15452,15453,15455,15456,15458,15459,15460,15461,15462,15463,15464,15465,15466,15467,15475,15476,15477,15478,15479,15481,15482,15484,15485,15486,15487,15488,15489,15490,15491,15492,15493,15494,15495,15496,15497,15498,15499,15500,15501,15502,15503,15504,15505,15506,15507,15508,15509,15510,15511,15512,15513,15514,15515,15516,15517,15518,15519,15520,15521,15522,15523,15524,15525,15526,15527,15528,15529,15530,15531,15532,15533,15534,15535,15536,15537,15538,15540,15541,15542,15543,15545,15546,15547,15548,15549,15550,15551,15552,15553,15554,15556,15557,15558,15559,15560,15561,15562,15563,15564,15565,15566,15567,15568,15569,15570,15571,15572,15573,15574,15576,15577,15578,15579,15580,15581,15582,15583,15584,15585,15586,15587,15588,15589,15590,15591,15592,15593,15594,15595,15596,15597,15598,15599,15600,15601,15602,15603,15604,15605,15606,15607,15608,15609,15610,15611,15612,15613,15614,15615,15616,15617,15618,15619,15620,15621,15622,15623,15624,15625,15626,15627,15628,15629,15630,15631,15632,15633,15634,15636,15639,15640,15641,15642,15643,15644,15645,15646,15647,15648,15649,15650,15651,15653,15654,15655,15656,15657,15658,15659,15660,15661,15662,15663,15664,15665,15666,15667,15668,15669,15670,15671,15672,15673,15674,15675,15676,15677,15678,15679,15680,15681,15682,15683,15684,15685,15686,15687,15688,15689,15690,15691,15692,15693,15694,15695,15696,15697,15698,15699,15700,15701,15702,15703,15704,15705,15706,15707,15708,15709,15710,15711,15712,15713,15714,15715,15716,15717,15718,15719,15720,15721,15722,15723,15725,15726,15727,15728,15729,15730,15731,15732,15733,15734,15736,15737,15738,15739,15740,15741,15742,15743,15744,15745,15746,15747,15748,15749,15750,15751,15752,15753,15754,15756,15757,15758,15759,15760,15761,15762,15763,15764,15765,15766,15767,15768,15769,15770,15771,15772,15773,15774,15775,15776,15777,15778,15779,15780,15781,15782,15783,15784,15785,15786,15787,15788,15789,15790,15791,15792,15793,15794,15795,15796,15797,15798,15799,15800,15801,15802,15803,15804,15805,15806,15807,15808,15809,15810,15811,15812,15813,15814,15815,15816,15817,15819,15820,15821,15822,15823,15824,15825,15826,15827,15828,15829,15830,15831,15832,15833,15834,15835,15836,15837,15838,15839,15840,15841,15842,15843,15844,15845,15847,15848,15850,15851,15853,15854,15855,15856,15857,15858,15859,15860,15861,15862,15870,15871,15872,15873,15874,15875,15876,15877,15878,15879,15880,15881,15882,15883,15884,15885,15886,15887,15888,15889,15890,15891,15892,15893,15894,15895,15896,15897,15898,15899,15900,15901,15902,15903,15904,15905,15906,15907,15908,15909,15910,15911,15912,15913,15914,15915,15916,15917,15918,15920,15921,15922,15923,15924,15925,15926,15927,15928,15929,15930,15931,15932,15933,15934,15935,15936,15937,15938,15939,15940,15941,15942,15943,15944,15945,15946,15947,15948,15949,15950,15951,15952,15953,15954,15955,15957,15958,15960,15961,15963,15964,15965,15966,15967,15968,15969,15970,15971,15972,15980,15981,15982,15983,15984,15985,15986,15987,15988,15989,15990,15991,15992,15993,15994,15995,15996,15997,15998,15999,16000,16001,16002,16003,16004,16005,16006,16007,16008,16009,16010,16011,16012,16013,16014,16015,16016,16017,16018,16019,16020,16021,16022,16023,16024,16025,16026,16027,16028,16029,16030,16031,16032,16033,16034,16035,16036,16038,16039,16040,16041,16042,16043,16044,16045,16046,16047,16048,16049,16050,16051,16052,16053,16054,16055,16056,16057,16058,16059,16060,16061,16062,16063,16064,16065,16066,16067,16068,16069,16070,16071,16072,16073,16075,16076,16078,16079,16081,16082,16083,16084,16085,16086,16087,16088,16089,16090,16098,16099,16100,16101,16102,16103,16104,16105,16106,16107,16108,16109,16110,16111,16112,16113,16114,16115,16116,16117,16118,16119,16120,16121,16122,16123,16124,16125,16126,16127,16128,16129,16130,16131,16132,16133,16134,16135,16136,16137,16138,16139,16140,16141,16142,16143,16144,16145,16146,16147,16148,16149,16150,16151,16152,16153,16154,16157,16158,16159,16160,16161,16162,16163,16164,16165,16166,16167,16168,16169,16170,16171,16172").split(
                ",");


//        List<Long> callListId = Arrays.stream(callListIdArray).map(Long::parseLong).toList();

//        List<MethodCallExpression> actualList = daoService.getMethodCallExpressionToMockFast(callListId);
//        List<MethodCallExpression> expectedList = daoService.getMethodCallExpressionToMock(callListId);

//        Assertions.assertEquals(expectedList, actualList);
    }
}